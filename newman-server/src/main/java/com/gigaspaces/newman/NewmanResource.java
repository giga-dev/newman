package com.gigaspaces.newman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.config.Config;
import com.gigaspaces.newman.dao.*;
import com.gigaspaces.newman.utils.FileUtils;
import com.mongodb.MongoClient;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Singleton
@Path("newman")
@PermitAll
public class NewmanResource {
    private static final Logger logger = LoggerFactory.getLogger(NewmanResource.class);
    private static final String MODIFIED_BUILD = "modified-build";
    public static final String CREATED_TEST = "created-test";
    public static final String MODIFIED_JOB = "modified-job";
    public static final String MODIFIED_TEST = "modified-test";
    public static final String CREATED_JOB = "created-job";
    public static final String MODIFIED_AGENT = "modified-agent";
    public static final String CREATED_OFFLINE_AGENT = "created-offline-agent";
    public static final String DELETED_OFFLINE_AGENT = "deleted-offline-agent";
    public static final String DELETED_AGENT = "deleted-agent";
    public static final String MODIFIED_FAILING_AGENTS = "modified-failing-agents";
    public static final String MODIFIED_AGENTS_COUNT = "modified-agents-count";
    public static final String CREATED_BUILD = "created-build";
    public static final String CREATED_SUITE = "created-suite";
    public static final String CREATED_JOB_CONFIG = "created-job-config";
    public static final String MODIFIED_SUITE = "modified-suite";
    public static final String CREATE_FUTURE_JOB = "created-future-job";
    public static final String DELETED_FUTURE_JOB = "deleted-future-job";
    private static final String MODIFY_SERVER_STATUS = "modified-server-status";

    private final MongoClient mongoClient;
    private final JobDAO jobDAO;
    private final TestDAO testDAO;
    private final BuildDAO buildDAO;
    private final AgentDAO agentDAO;
    private final SuiteDAO suiteDAO;
    private final JobConfigDAO jobConfigDAO;
    private final FutureJobDAO futureJobDAO;
    private final BuildsCacheDAO buildsCacheDAO;
    private final Config config;
    private static final String SERVER_TESTS_UPLOAD_LOCATION_FOLDER = "tests-logs";
    private static final String SERVER_JOBS_UPLOAD_LOCATION_FOLDER = "job-setup-logs";
    private static final String SERVER_CACHE_BUILDS_FOLDER = "builds";

    @SuppressWarnings("FieldCanBeLocal")
    private final Timer timer = new Timer(true);

    private final ConcurrentHashMap<String, Object> agentLocks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, OfflineAgent> offlineAgents = new ConcurrentHashMap<>();

    private static final int maxJobsPerSuite = 5;
    private final DistinctIterable distinctTestsByAssignedAgentFilter;

    private final static String CRITERIA_PROP_NAME = "criteria";

    final static String MASTER_BRANCH_NAME = "master";

    private final static boolean buildCacheEnabled = Boolean.getBoolean("newman.server.enabledBuildCache");
    private static String HTTP_WEB_ROOT_PATH;
    private static String HTTPS_WEB_ROOT_PATH;

    private static final Object takenTestLock = new Object();
    private final AtomicLong latestLogSize = new AtomicLong(0);
    private final AtomicLong lastLogSizeCheckTime = new AtomicLong(0);


    private final Object serverStatusLock = new Object();
    private ServerStatus serverStatus = new ServerStatus(ServerStatus.Status.RUNNING);
    private Thread serverSuspendThread;

    public NewmanResource(@Context ServletContext servletContext) {
        this.config = Config.fromString(servletContext.getInitParameter("config"));
        //noinspection SpellCheckingInspection
        mongoClient = new MongoClient(config.getMongo().getHost());
        Morphia morphia = initMorphia();
        Datastore ds = morphia.createDatastore(mongoClient, config.getMongo().getDb());
        ds.ensureIndexes();
        ds.ensureCaps();
        jobDAO = new JobDAO(morphia, mongoClient, config.getMongo().getDb());
        testDAO = new TestDAO(morphia, mongoClient, config.getMongo().getDb());
        buildDAO = new BuildDAO(morphia, mongoClient, config.getMongo().getDb());
        agentDAO = new AgentDAO(morphia, mongoClient, config.getMongo().getDb());
        suiteDAO = new SuiteDAO(morphia, mongoClient, config.getMongo().getDb());
        futureJobDAO = new FutureJobDAO(morphia, mongoClient, config.getMongo().getDb());
        buildsCacheDAO = new BuildsCacheDAO(morphia, mongoClient, config.getMongo().getDb());
        jobConfigDAO = new JobConfigDAO(morphia, mongoClient, config.getMongo().getDb());

        MongoDatabase db = mongoClient.getDatabase(config.getMongo().getDb());
        MongoCollection testCollection = db.getCollection("Test");
        distinctTestsByAssignedAgentFilter = testCollection.distinct("assignedAgent", String.class);

        if (Boolean.getBoolean("production")) { // This is set to true in the newman server
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.info("Checking for not seen agents");
                    getAgentsNotSeenInLastMillis(1000 * 60 * 3).forEach(NewmanResource.this::handleUnseenAgent);
                }
            }, 1000 * 30, 1000 * 30);

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.info("Checking for zombie agents");
                    getZombieAgents(1000 * 60 * 20).forEach(NewmanResource.this::handleZombieAgent);
                }
            }, 1000 * 30, 1000 * 30);

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    handleHangingJob();
                }
            }, 1000 * 30, 1000 * 30);
        }

        initBuildsCache();

        initWebRootsPath();
    }

    private void handleHangingJob() {
        Job potentialJob = getPotentialJob();
        if (potentialJob == null) {
            // might happand when there are no Jobs that should run (Ready/Running)
            return;
        }
        if (!handleSetupProblem(potentialJob)) {
            handleZombie(potentialJob);
        }
    }

    private boolean handleSetupProblem(Job potentialJob) {
        int maxPrepareTimeHours = 1;
        if (tooLongPrepareTime(potentialJob, maxPrepareTimeHours) && potentialJob.getState().equals(State.READY)) {
            logger.info("Job state is BROKEN because it had setup problem for {} hours. job - id:[{}], name: [{}], build:[{}], startPrepareTime: [{}].",
                    maxPrepareTimeHours, potentialJob.getId(), potentialJob.getSuite().getName(), potentialJob.getBuild().getName(), potentialJob.getStartPrepareTime());
            updateBrokenJob(potentialJob);
            return true;
        }
        return false;
    }

    private void updateBrokenJob(Job potentialJob) {
        potentialJob.setState(State.BROKEN);
        updateJob(potentialJob.getId(), potentialJob);

        UpdateOperations<Build> buildUpdateOperations = buildDAO.createUpdateOperations();
        buildUpdateOperations.inc("buildStatus.brokenJobs").dec("buildStatus.runningJobs");
        buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(potentialJob.getBuild().getId()), buildUpdateOperations);
    }

    private boolean handleZombie(Job potentialJob) {
        if (potentialJob.getLastTimeZombie() == null) {
            if (isZombie(potentialJob)) { // job not running and zombie
                UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations();
                updateJobStatus.set("lastTimeZombie", new Date());
                jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(potentialJob.getId()), updateJobStatus);
                return true;
            }
        } else { // already seen as zombie
            int hoursToWaitBeforeDelete = 1;
            if (isTimeExpired(potentialJob.getLastTimeZombie().getTime(), hoursToWaitBeforeDelete, TimeUnit.HOURS)) {
                logger.info("Job state is BROKEN because it became zombie (no match agents) for {} hours. job: [id:{}, name: {}, build:{}].",
                        hoursToWaitBeforeDelete, potentialJob.getId(), potentialJob.getSuite().getName(), potentialJob.getBuild().getName());
                updateBrokenJob(potentialJob);
                return true;
            }
        }
        return false;
    }

    private boolean isTimeExpired(long startTimeMilliseconds, int timeToPass, TimeUnit timeUnit) {
        long divideByTineUnit = 1;
        if (timeUnit.equals(TimeUnit.SECONDS)) {
            divideByTineUnit = 1000;
        } else if (timeUnit.equals(TimeUnit.MINUTES)) {
            divideByTineUnit = 1000 * 60;
        } else if (timeUnit.equals(TimeUnit.HOURS)) {
            divideByTineUnit = 1000 * 60 * 60;
        }
        long currentTime = System.currentTimeMillis();
        long timePassSinceStart = currentTime - startTimeMilliseconds;
        int timePassed = (int) (timePassSinceStart / divideByTineUnit);

        return timePassed >= timeToPass;
    }

    private Job getPotentialJob() {
        Set<String> allCapabilities = allNecessaryCapabilities();
        Query<Job> basicDummyQuery = basicJobQuery();
        return findJob(allCapabilities, basicDummyQuery);
    }

    private Query<Job> basicJobQuery() {
        Query<Job> basicDummyQuery = jobDAO.createQuery();
        basicDummyQuery.or(basicDummyQuery.criteria("state").equal(State.READY), basicDummyQuery.criteria("state").equal(State.RUNNING));
        basicDummyQuery.where("(this.totalTests + this.numOfTestRetries) != (this.passedTests + this.failedTests + this.runningTests)");
        basicDummyQuery.order("submitTime");
        return basicDummyQuery;
    }

    private Set<String> allNecessaryCapabilities() {
        // create capabilities from requirements, because might be requirements without suitable agents
        Query<Suite> suitesQuery = suiteDAO.createQuery();
        suitesQuery.retrievedFields(true, "requirements");
        QueryResults<Suite> suites = suiteDAO.find(suitesQuery);
        Set<String> allCapabilities = new HashSet<>();
        for (Suite suite : suites) {
            allCapabilities.addAll(suite.getRequirements());
        }
        return allCapabilities;
    }

    private boolean tooLongPrepareTime(Job potentialJob, int maxPrepareTimeHours) {
        if (potentialJob.getStartPrepareTime() != null) {
            long firstTimePrepare = potentialJob.getStartPrepareTime().getTime();
            long currentTime = System.currentTimeMillis();
            long timePassSinceFirstPrepare = currentTime - firstTimePrepare;
            int hoursPassed = (int) (timePassSinceFirstPrepare / (1000 * 60 * 60));

            if (hoursPassed >= maxPrepareTimeHours) {
                return true;
            }

            if (isTimeExpired(potentialJob.getStartPrepareTime().getTime(), maxPrepareTimeHours, TimeUnit.HOURS)) {
                return true;
            }
        }
        return false;
    }

    private boolean isZombie(Job job) {
        QueryResults<Agent> agents = agentDAO.find(agentDAO.createQuery());
        // check if there is an agent in the system that can execute this job
        for (Agent agent : agents) {
            if (job.getSuite().getRequirements().isEmpty()
                    || job.getSuite().getRequirements() == null
                    || agent.getCapabilities().containsAll(job.getSuite().getRequirements())) {
                return false;
            }
        }
        return true;
    }

    private Morphia initMorphia() {
        Morphia morphia;
        try {
            morphia = new Morphia().mapPackage("com.gigaspaces.newman.beans.criteria").mapPackage("com.gigaspaces.newman.beans");
        } catch (Exception e) {
            logger.error("failed to init morphia", e);
            throw e;
        }
        return morphia;
    }

    private void initWebRootsPath() {
        if (HTTP_WEB_ROOT_PATH == null && HTTPS_WEB_ROOT_PATH == null) {
            String address = System.getProperty("newman.server.address");
            if (address == null) {
                try {
                    address = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException ignored) {
                    address = "localhost";
                }
            }
            HTTP_WEB_ROOT_PATH = "http://" + address + ":8080/api/newman";
            HTTPS_WEB_ROOT_PATH = "https://" + address + ":8443/api/newman";
            logger.info("initialized http web root path: {}", HTTP_WEB_ROOT_PATH);
            logger.info("initialized https web root path: {}", HTTPS_WEB_ROOT_PATH);
        }
    }

    private void initBuildsCache() {
        BuildsCache found = buildsCacheDAO.findOne(buildsCacheDAO.createQuery());
        if (found == null) {
            buildsCacheDAO.save(new BuildsCache());
        }
    }

    @GET
    @Path("update-sha")
    @Produces(MediaType.TEXT_PLAIN)
    public String updateSha() {
        logger.info("updating SHA for all tests ...");
        int records = 0;
        Query<Test> query = testDAO.createQuery();
        for (Test test : testDAO.find(query)) {
            if (test.getSha() == null) {
                Job job = getJob(test.getJobId());
                UpdateOperations<Test> ops = testDAO.createUpdateOperations().set("sha", Sha.compute(test.getName(), test.getArguments(), job.getSuite().getId(), job.getBuild().getBranch()));
                testDAO.updateFirst(testDAO.createIdQuery(test.getId()), ops);
                ++records;
            }
        }
        logger.info("updating done {} records were updated", records);

        return String.valueOf(records);
    }

    @GET
    @Path("jobs-view")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<JobView> jobsView(@DefaultValue("0") @QueryParam("offset") int offset,
                                   @DefaultValue("30") @QueryParam("limit") int limit
            , @QueryParam("buildId") String buildId
            , @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

        List<Job> jobs = retrieveJobs(buildId, orderBy, all, offset, limit, true);
        List<JobView> jobViews = new ArrayList<>(jobs.size());

        for (Job job : jobs) {
            jobViews.add(new JobView(job));
        }
        return new Batch<>(jobViews, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("all-builds-and-suites")
    @Produces(MediaType.APPLICATION_JSON)
    public AllSuitesAndBuilds getAllBuildsAndSuites() {

        List<Suite> suites = getAllThinSuites(true);//  suiteDAO.find( suitesQuery ).asList();

        List<SuiteView> suiteViews = new ArrayList<>(suites.size());
        for (Suite suite : suites) {
            suiteViews.add(new SuiteView(suite));
        }

        List<Build> builds = getThinBuilds();
        int buildsCount = builds.size();
        List<BuildView> buildViews = new ArrayList<>(buildsCount);
        for (Build build : builds) {
            buildViews.add(new BuildView(build));
        }
        return new AllSuitesAndBuilds(buildViews, suiteViews);
    }

    private List<Build> getThinBuilds() {
        final int buildsLimit = 30;
        Query<Build> buildsQuery = buildDAO.createQuery();

        buildsQuery.retrievedFields(true, "id", "name", "branch", "tags");
        buildsQuery.order("-buildTime").limit(buildsLimit);
        List<Build> builds = buildDAO.find(buildsQuery).asList();
        return builds;
    }


    @GET
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Job> jobs(@DefaultValue("0") @QueryParam("offset") int offset,
                           @DefaultValue("30") @QueryParam("limit") int limit
            , @QueryParam("buildId") String buildId
            , @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

        List<Job> jobs = retrieveJobs(buildId, orderBy, all, offset, limit, false);
        return new Batch<>(jobs, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("job/running")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRunningJobs() {
        Query<Job> query = jobDAO.createQuery();
        query.or(query.criteria("state").equal("READY"), query.criteria("state").equal("RUNNING"));
        query.and(query.criteria("totalTests").greaterThan(0));
        return String.valueOf(jobDAO.find(query).asList().size());
    }

    private List<Job> retrieveJobs(String buildId, List<String> orderBy, boolean all, int offset, int limit, boolean returnThinObjects) {

        Query<Job> query = jobDAO.createQuery();
        if (buildId != null) {
            query.field("build.id").equal(buildId);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }
        if (!all) {
            query.offset(offset).limit(limit);
        }

        if (returnThinObjects) {
            addRequiredJobTableColumns(query);
        }

        return jobDAO.find(query).asList();
    }


    private void addRequiredBuildTableColumns(Query<Build> query) {
        query.retrievedFields(false, "resources", "testsMetadata", "shas", "tags");
    }

    private void addRequiredJobTableColumns(Query<Job> query) {
        query.retrievedFields(true, "id", "build.id", "build.name", "build.branch", "suite.id", "suite.name", "jobConfig.id", "jobConfig.name",
                "submitTime", "startTime", "endTime", "testURI", "submittedBy", "state", "totalTests",
                "passedTests", "failedTests", "failed3TimesTests", "runningTests", "numOfTestRetries", "preparingAgents");
    }


    @GET
    @Path("futureJob")
    @Produces(MediaType.APPLICATION_JSON)
    public FutureJob getAndDeleteFutureJob(@Context UriInfo uriInfo) {
        checkServerStatus();

        Query<FutureJob> query = futureJobDAO.createQuery();
        query.order("submitTime");
        FutureJob futureJob = futureJobDAO.findOne(query);
        if (futureJob != null) {
            Datastore datastore = futureJobDAO.getDatastore();
            datastore.findAndDelete(query);
            broadcastMessage(DELETED_FUTURE_JOB, futureJob);
        }
        return futureJob;
    }

    @DELETE
    @Path("deleteFutureJob/{futureJobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFutureJob(final @PathParam("futureJobId") String futureJobId) {

        Query<FutureJob> query = futureJobDAO.createIdQuery(futureJobId);
        FutureJob futureJob = futureJobDAO.findOne(query);
        if (futureJob != null) {
            Datastore datastore = futureJobDAO.getDatastore();
            datastore.findAndDelete(query);
            broadcastMessage(DELETED_FUTURE_JOB, futureJob);
        }

        return Response.ok(Entity.json(futureJobId)).build();
    }

    @GET
    @Path("job/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job getJob(@PathParam("id") final String id) {

        Job job = findOneJobById(id);
        if (job != null) {
            Bson jobIdFilter = Filters.eq("jobId", id);
            long filterTestsStart = System.currentTimeMillis();
            DistinctIterable assignedAgents = distinctTestsByAssignedAgentFilter.filter(jobIdFilter);
            //noinspection unchecked
            Set<String> agents = (Set) assignedAgents.into(new HashSet<>());
            job.setAgents(agents);
            long filterTestsEnd = System.currentTimeMillis();

            if (logger.isDebugEnabled()) {
                logger.debug("distinct filter by job id took {} msec.", (filterTestsEnd - filterTestsStart));
            }
        }

        return job;
    }

    private Job findOneJobById(String jobId) {
        Query<Job> query = jobDAO.createQuery();
        return jobDAO.findOne(query.field("_id").equal(new ObjectId(jobId)));
    }

    private Job findOneThinJobById(String jobId) {
        Query<Job> query = jobDAO.createQuery();
        query.retrievedFields(true, "id", "suite.id", "suite.name", "build.id", "build.name", "build.branch");
        return jobDAO.findOne(query.field("_id").equal(new ObjectId(jobId)));
    }


    @DELETE
    @Path("jobs/{nubmerOfDaysToNotDelete}")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String deleteJobUntilDesiredSpace(final @PathParam("nubmerOfDaysToNotDelete") String nubmerOfDaysToNotDelete) throws InterruptedException {
        int numberOfDays = Integer.parseInt(nubmerOfDaysToNotDelete);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -numberOfDays);
        Date deleteUntilDate = cal.getTime();
        Query<Build> filteredQuery = buildDAO.createQuery().field("buildTime").lessThan(deleteUntilDate);
        List<Build> buildList = buildDAO.find(filteredQuery).asList();
        int jobsDeleted = 0;
        for (Build build : buildList) {
            Query<Job> query = jobDAO.createQuery();
            query.field("build.id").equal(build.getId());
            List<Job> jobs = jobDAO.find(query).asList();
            for (Job job : jobs) {
                if (!job.getState().equals(State.DONE)) {
                    continue;
                }
                deleteJob(job.getId());
                if( logger.isDebugEnabled() ) {
                    logger.debug(
                        "deleted job: " + job.getId() + " with build time " + job.getBuild()
                            .getBuildTime());
                }
                jobsDeleted++;
            }
        }
        return "Deleted: " + jobsDeleted + " jobs from the last " + numberOfDays + " days.";
    }


    @PUT
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createJob(JobRequest jobRequest, @Context SecurityContext sc) {
        checkServerStatus();

        Build build = buildDAO.findOne(buildDAO.createIdQuery(jobRequest.getBuildId()));
        JobConfig jobConfig = jobConfigDAO.findOne(jobConfigDAO.createIdQuery(jobRequest.getConfigId()));
        Suite suite = null;

        if (jobRequest.getSuiteId() != null) {
            suite = suiteDAO.findOne(suiteDAO.createIdQuery(jobRequest.getSuiteId()));
        }
        if (suite == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("invalid suite id for Job request: " + jobRequest).build();
        }

        if (build != null) {
            Job job = new Job();
            job.setBuild(build);
            job.setSuite(suite);
            job.setState(State.READY);
            job.setSubmitTime(new Date());
            job.setSubmittedBy(jobRequest.getAuthor());
            if(jobConfig !=null) {
                job.setJobConfig(jobConfig);
            }
            jobDAO.save(job);
            UpdateOperations<Build> buildUpdateOperations = buildDAO.createUpdateOperations().inc("buildStatus.totalJobs")
                    .inc("buildStatus.pendingJobs")
                    .add("buildStatus.suitesIds", suite.getId(), false)
                    .add("buildStatus.suitesNames", suite.getName(), false);
            build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(build.getId()), buildUpdateOperations);
            broadcastMessage(CREATED_JOB, job);
            broadcastMessage(MODIFIED_BUILD, build);
            return Response.ok(job).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("futureJob/{buildId}/{suiteId}/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    public FutureJob createFutureJob(
            @PathParam("buildId") String buildId,
            @PathParam("suiteId") String suiteId,
            @PathParam("configId") String configId,
            @QueryParam("author") String authorOpt,
            @Context SecurityContext sc) {
        checkServerStatus();

        String author = (authorOpt != null && authorOpt.length() > 0 ? authorOpt : sc.getUserPrincipal().getName());
        Build build = null;
        Suite suite = null;
        JobConfig jobConfig = null;

        if (buildId != null) {
            build = buildDAO.findOne(buildDAO.createIdQuery(buildId));
            if (build == null) {
                throw new BadRequestException("invalid build id in create FutureJob: " + buildId);
            }
        }
        if (suiteId != null) {
            suite = suiteDAO.findOne(suiteDAO.createIdQuery(suiteId));
            if (suite == null) {
                throw new BadRequestException("invalid suite id in create FutureJob: " + suiteId);
            }
        }
        if (configId != null) {
            jobConfig = jobConfigDAO.findOne(jobConfigDAO.createIdQuery(configId));
            if (jobConfig == null) {
                throw new BadRequestException("invalid config id in create FutureJob: " + configId);
            }
        }

        //noinspection ConstantConditions
        FutureJob futureJob = new FutureJob(build.getId(), build.getName(), build.getBranch(), suite.getId(), suite.getName(),jobConfig.getId(),jobConfig.getName(), author);

        futureJobDAO.save(futureJob);
        broadcastMessage(CREATE_FUTURE_JOB, futureJob);
        return futureJob;
    }

    @POST
    @Path("futureJob")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<FutureJob> createFutureJobs(
            FutureJobsRequest futureJobsRequest,
            @QueryParam("author") String authorOpt,
            @Context SecurityContext sc) {
        checkServerStatus();

        String author = (authorOpt != null && authorOpt.length() > 0 ? authorOpt : sc.getUserPrincipal().getName());
        Build build = null;
        JobConfig jobConfig = null;
        List<String> suites = futureJobsRequest.getSuites();
        String buildId = futureJobsRequest.getBuildId();
        String configId = futureJobsRequest.getConfigId();

        if (buildId != null) {
            build = buildDAO.findOne(buildDAO.createIdQuery(buildId));
            if (build == null) {
                throw new BadRequestException("invalid build id in create FutureJob: " + buildId);
            }
        }
        if (configId != null) {
            jobConfig = jobConfigDAO.findOne(jobConfigDAO.createIdQuery(configId));
            if (jobConfig == null) {
                throw new BadRequestException("invalid config id in create FutureJob: " + configId);
            }
        }
        List<FutureJob> response = new ArrayList<FutureJob>();
        if (!suites.isEmpty()) {
            for (String suiteId : suites) {
                Suite suite = suiteDAO.findOne(suiteDAO.createIdQuery(suiteId));
                if (suite == null) {
                    throw new BadRequestException("invalid suite id in create FutureJob: " + suiteId);
                }

                //noinspection ConstantConditions
                FutureJob futureJob = new FutureJob(build.getId(), build.getName(), build.getBranch(), suite.getId(), suite.getName(), jobConfig.getId(),jobConfig.getName(),author);
                response.add(futureJob);

                futureJobDAO.save(futureJob);
                broadcastMessage(CREATE_FUTURE_JOB, futureJob);

            }
        }
        return response;
    }

    @POST
    @Path("job/{id}/toggle")
    @Produces(MediaType.APPLICATION_JSON)
    public Job toggelJobPause(@PathParam("id") final String id) {
        Job job = findOneJobById(id);
        if (job != null) {
            State state = null;
            State old = job.getState();
            switch (job.getState()) {
                case READY:
                case RUNNING:
                    state = State.PAUSED;
                    break;
                case PAUSED:
                    state = State.READY;
                    break;
                case DONE:
                    break;
            }
            if (state != null) {
                UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().set("state", state);
                if (state.equals(State.PAUSED)) {
                    // remove startPrepareTime after turn job to paused because after pause agents do setup again on job
                    updateJobStatus.unset("startPrepareTime");
                }
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(job.getId()).field("state").equal(old), updateJobStatus);
                broadcastMessage(MODIFIED_JOB, job);

                //change status of Test(s) from running to pending
                if (state == State.READY) {
                    Query<Test> query = testDAO.createQuery();
                    query.and(query.criteria("jobId").equal(id), query.criteria("status").equal(Test.Status.RUNNING));
                    UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("status", Test.Status.PENDING);

                    UpdateResults update = testDAO.getDatastore().update(query, updateOps);
                    logger.info("---ToggleJobPause, state is READY, affected count:" + update.getUpdatedCount());
                }
                return job;
            }
        }
        return null;
    }

    @POST
    @Path("jobs/pause")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Job> toggelMultipleJobPause(final List<String> ids) {
        List<Job> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Job job = findOneJobById(id);
            if (job != null) {
                State state = null;
                State old = job.getState();
                switch (job.getState()) {
                    case READY:
                    case RUNNING:
                        state = State.PAUSED;
                        break;
                    case PAUSED:
                        break;
                    case DONE:
                        break;
                }
                if (state != null) {
                    UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().set("state", state);
                    if (state.equals(State.PAUSED)) {
                        // remove startPrepareTime after turn job to paused because after pause agents do setup again on job
                        updateJobStatus.unset("startPrepareTime");
                    }
                    job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(job.getId()).field("state").equal(old), updateJobStatus);
                    result.add(job);
                    broadcastMessage(MODIFIED_JOB, job);
                }
            }
        }
        return result;
    }

    @POST
    @Path("jobs/resume")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Job> toggelMultipleJobResume(final List<String> ids) {
        List<Job> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Job job = findOneJobById(id);
            if (job != null) {
                State state = null;
                State old = job.getState();
                switch (job.getState()) {
                    case PAUSED:
                        state = State.READY;
                        break;
                    case READY:
                        break;
                    case RUNNING:
                        break;
                    case DONE:
                        break;
                }
                if (state != null) {
                    UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().set("state", state);
                    job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(job.getId()).field("state").equal(old), updateJobStatus);
                    result.add(job);
                    broadcastMessage(MODIFIED_JOB, job);

                    //change status of Test(s) from running to pending
                    Query<Test> query = testDAO.createQuery();
                    query.and(query.criteria("jobId").equal(id), query.criteria("status").equal(Test.Status.RUNNING));
                    UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("status", Test.Status.PENDING);

                    UpdateResults update = testDAO.getDatastore().update(query, updateOps);
                    logger.info("---toggelMultipleJobResume, state is READY, affected count:" + update.getUpdatedCount());
                }
            }
        }
        return result;
    }

    @GET
    @Path("dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardData getActiveJobGroup(@Context UriInfo uriInfo) {

        Query<Build> activeBuildsQuery = buildDAO.createQuery();
        addRequiredBuildTableColumns(activeBuildsQuery);
        List<Build> activeBuilds =
                buildDAO.find(activeBuildsQuery.
                        where("this.buildStatus.runningJobs>0").
                        where("this.buildStatus.totalJobs>0").
                        where("this.buildStatus.doneJobs + this.buildStatus.brokenJobs < this.buildStatus.totalJobs").
                        order("-buildTime")).
                        asList();

        Query<Build> pendingBuildsQuery = buildDAO.createQuery();
        addRequiredBuildTableColumns(pendingBuildsQuery);
        List<Build> pendingBuilds =
                buildDAO.find(pendingBuildsQuery.
                        where("this.buildStatus.pendingJobs>0").
                        where("this.buildStatus.runningJobs<=0").
                        where("this.buildStatus.totalJobs>0").
                        where("this.buildStatus.doneJobs + this.buildStatus.brokenJobs < this.buildStatus.totalJobs").
                        limit(5).
                        order("-buildTime")).
                        asList();

        Query<Build> historyBuildsQuery = buildDAO.createQuery();
        addRequiredBuildTableColumns(historyBuildsQuery);
        List<Build> historyBuilds =
                buildDAO.find(historyBuildsQuery.
                        where("this.buildStatus.totalJobs>0").
                        where("this.buildStatus.doneJobs + this.buildStatus.brokenJobs == this.buildStatus.totalJobs").
                        limit(5).
                        order("-buildTime")).
                        asList();
        Query<FutureJob> futureJobQuery = futureJobDAO.createQuery();
        List<FutureJob> futureJobs = futureJobDAO.find(futureJobQuery).asList();

        Map<String, List<Job>> activeJobsMap = createActiveJobsMap(activeBuilds);

        return new DashboardData(activeBuilds, pendingBuilds, historyBuilds, activeJobsMap, futureJobs);
    }

    @GET
    @Path("build/history")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<BuildWithJobs> getHistoryJobGroup(@Context UriInfo uriInfo) {
        Query<Job> query = jobDAO.createQuery();
        query.criteria("state").equal(State.DONE);
        query.field("build.id").hasNoneOf(Arrays.asList("foo", "bar"));
        query.order("-submitTime");
        Iterator<Job> iterator = jobDAO.find(query).iterator();
        while (iterator.hasNext()) {
            Job next = iterator.next();
            logger.info("job {}", next);
        }
        ((MorphiaIterator) iterator).close();
        return null;
    }


    @POST
    @Path("unsubscribe")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job unsubscribe(final Agent agent) {
        String jobId = agent.getJobId();
        Job job = null;
        if (jobId == null) {
            throw new BadRequestException("can't unsubscribe agent without a job " + agent);
        }
        if (agent.getState() == Agent.State.PREPARING) {
            job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId), jobDAO.createUpdateOperations().removeAll("preparingAgents", agent.getName()));
            broadcastMessage(MODIFIED_JOB, job);
        }
        UpdateOperations<Agent> updateAgentOps = agentDAO.createUpdateOperations().unset("jobId");
        agentDAO.getDatastore().updateFirst(agentDAO.createQuery().field("name").equal(agent.getName()), updateAgentOps, true);
        broadcastMessage(MODIFIED_AGENT, agent);
        return job;
    }

    @POST
    @Path("freeAgent/{agentName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Agent freeAgent(@PathParam("agentName") final String agentName) {
        Agent agent = agentDAO.findOne(agentDAO.createQuery().field("name").equal(agentName));
        if (agent != null) {
            returnTests(agent);
            handleZombieAgent(agent);
        }
        return agent;
    }

    @PUT
    @Path("tests")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addTests(Batch<Test> tests, @QueryParam("toCount") String toCountStr) {
        if (tests.getValues().isEmpty()) {
            return;
        }
        Job job = jobDAO.findOne(jobDAO.createIdQuery(tests.getValues().get(0).getJobId()));
        if (job == null) {
            return;
        }
        List<Test> res = new ArrayList<>(tests.getValues().size());
        for (Test test : tests.getValues()) {
            res.add(addTest(test));
        }
        if (!res.isEmpty()) {
            Test test = res.get(0);

            // @QueryParam("toCount") - indicates if tests should be added to "totalTests"
            // if a test fails and is resumitted (aka run number >1 ) we don't want it to be added to "totalTests".
            if (toCountStr.equals("count")) {
                UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations().inc("totalTests", res.size());
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(test.getJobId()), jobUpdateOps, false, false);
                Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()), buildDAO.createUpdateOperations().inc("buildStatus.totalTests", res.size()));
                broadcastMessage(MODIFIED_BUILD, build);
                broadcastMessage(MODIFIED_JOB, job);
            }
        }
    }


    // return all builds that need to be submit, e.g there are NO jobs that run with them
    // each cell on array represent different branch
    @GET
    @Path("build-to-submit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Build getPendingBuildsToSubmit(
            @QueryParam("branch") String branchStr,
            @QueryParam("tags") String tagsStr,
            @QueryParam("mode") String modeStr) {

        if (modeStr == null || modeStr.isEmpty()) {
            modeStr = "DAILY";
        }

        logger.info("choosing build with params - branchStr [" + branchStr + "], tagsStr [" + tagsStr + "], modeStr [" + modeStr + "]");

        Build buildsRes;
        Set<String> tagsSet = null;
        if (branchStr == null || branchStr.isEmpty()) {
            throw new IllegalArgumentException("branch can not be null or empty when asking for pending build to submit");
        }

        if (tagsStr != null && !tagsStr.isEmpty()) {
            tagsSet = new HashSet<>(Arrays.asList(tagsStr.split("\\s*,\\s*")));
        }

        if ("DAILY".equals(modeStr)) {

            Set<String> excludeTags = new HashSet<>();
            excludeTags.add("RELEASE");
            Query<Build> query = buildDAO.createQuery().order("-buildTime").field("branch").equal(branchStr).field("tags").hasNoneOf(excludeTags);

            if (tagsSet != null && !tagsSet.isEmpty()) { // build should be with specifics tags
                query.field("tags").hasAllOf(tagsSet);
            }

            Build build = buildDAO.findOne(query);
            if (build != null && build.getBuildStatus().getTotalJobs() == 0) {
                buildsRes = build;
            } else {
                String errorStr = "Did not find build with params - branchStr [" + branchStr + "], tagsStr [" + tagsStr + "], modeStr [" + modeStr + "]";
                logger.error(errorStr);
                throw new RuntimeException(errorStr);
            }
        } else if ("NIGHTLY".equals(modeStr)) {
            //get latest nightly build that didn't run, if exist
            buildsRes = getLatestReleaseBuild();
            logger.info("try find build - used getLatestReleaseBuild - build is [" + buildsRes + "]");
            // if nightly mode and there aren't new builds - take last build anyway
            if (buildsRes == null) {
                buildsRes = getLatestBuild(branchStr);
                logger.info("Did not find build trying for second time - used getLatestBuild - build is [" + buildsRes + "]");
            }
            buildsRes.addTag("NIGHTLY");
            updateBuild(buildsRes.getId(), buildsRes);
        } else {
            String errorStr = "Got unsupported build MODE [" + modeStr + "]";
            logger.error(errorStr);
            throw new RuntimeException(errorStr);
        }
        return buildsRes;
    }

    private Build getLatestReleaseBuild() {
        Build res = null;
        Query<Build> query;
        Set<String> xapReleaseTags = new HashSet<>();
        xapReleaseTags.add("RELEASE");
        xapReleaseTags.add("XAP");
        query = buildDAO.createQuery().order("-buildTime").field("tags").hasAllOf(xapReleaseTags);
        QueryResults<Build> queryResults = buildDAO.find(query);
        List<Build> builds = queryResults.asList();
        logger.info("choosing build in getLatestReleaseBuild method. found [" + builds.size() + "] builds, need to pick one.\n builds:");
        for (Build nightly : builds) {
            logger.info("build is id: [" + nightly.getId() + "], name: [" + nightly.getName() + "], branch: [" + nightly.getBranch() + "], " +
                    "tags: [" + nightly.getTags() + "]");
            if (nightly.getBuildStatus().getTotalJobs() == 0) {
                res = nightly;
                break;
            }
        }
        if (res != null) {
            logger.info("Chosen build(!) is id: [" + res.getId() + "], name: [" + res.getName() + "], branch: [" + res.getBranch() + "], " +
                    "tags: [" + res.getTags() + "]");
        } else {
            logger.info("Did not choose build in getLatestReleaseBuild [res is null]");
        }

        return res;
    }

    private Test addTest(Test test) {
        if (test.getJobId() == null) {
            throw new BadRequestException("can't add test with no jobId: " + test);
        }
        test.setStatus(Test.Status.PENDING);
        test.setScheduledAt(new Date());
        Job job = getJob(test.getJobId());
        test.setSha(Sha.compute(test.getName(), test.getArguments(), job.getSuite().getId(), job.getBuild().getBranch()));
        testDAO.save(test);
        broadcastMessage(CREATED_TEST, test);
        return test;
    }


    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public synchronized Test finishTest(final Test test) {
        try {
            if( logger.isDebugEnabled() ) {
                logger.debug("trying to finish test - id:[{}], name:[{}]", test.getId(),
                             test.getName());
            }
            if (test.getId() == null) {
                throw new BadRequestException("can't finish test without testId: " + test);
            }
            String jobId = test.getJobId();
            Job testJob = getJob(jobId);
            if (testJob == null) {
                throw new BadRequestException("finishTest - the job of the test is not on database. test: [" + test + "].");
            }
            Test.Status status = test.getStatus();
            if (status == null || (status != Test.Status.FAIL && status != Test.Status.SUCCESS)) {
                throw new BadRequestException("can't finish test without state set to success or fail state" + test);
            }
            UpdateOperations<Test> testUpdateOps = testDAO.createUpdateOperations();
            UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations();
            UpdateOperations<Build> updateBuild = buildDAO.createUpdateOperations();
            testUpdateOps.set("status", status);
            if (test.getRunNumber() == 1) {
                if (status == Test.Status.FAIL) {
                    updateJobStatus.inc("failedTests");
                    updateBuild.inc("buildStatus.failedTests");
                } else {
                    updateJobStatus.inc("passedTests");
                    updateBuild.inc("buildStatus.passedTests");
                }
            }
            if ((test.getRunNumber() > 1)) {
                updateJobStatus.inc("numOfTestRetries");
                updateBuild.inc("buildStatus.numOfTestRetries");
            }
            if ((test.getRunNumber() == 3) && (status == Test.Status.FAIL)) {
                updateJobStatus.inc("failed3TimesTests");
                updateBuild.inc("buildStatus.failed3TimesTests");
            }
            updateJobStatus.dec("runningTests");
            updateBuild.dec("buildStatus.runningTests");

            if (test.getErrorMessage() != null) {
                testUpdateOps.set("errorMessage", test.getErrorMessage());
            }
            if (status == Test.Status.FAIL || status == Test.Status.SUCCESS) {
                testUpdateOps.set("endTime", new Date());
            }
            int historyLength = 25;
            List<TestHistoryItem> testHistory = getTests(test.getId(), 0, historyLength, null).getValues();
            String historyStatsString = TestScoreUtils.decodeShortHistoryString(test, testHistory, test.getStatus(), testJob.getBuild()); // added current fail to history;
            double reliabilityTestScore = TestScoreUtils.score(historyStatsString);

            testUpdateOps.set("testScore", reliabilityTestScore);
            testUpdateOps.set("historyStats", historyStatsString);
            if( logger.isDebugEnabled() ) {
                logger.debug(
                    "got test history [{}] of test and prepare to update:  id:[{}], name:[{}], jobId:[{}], running tests before decrement:[{}]",
                    historyStatsString, test.getId(), test.getName(), jobId,
                    testJob.getRunningTests());
            }

            Test result = testDAO.getDatastore().findAndModify(testDAO.createIdQuery(test.getId()), testUpdateOps, false, false);
            Query<Test> query = testDAO.createQuery();
            query.and(query.criteria("jobId").equal(result.getJobId()),
                    query.or(query.criteria("status").equal(Test.Status.PENDING),
                            query.criteria("status").equal(Test.Status.RUNNING)));
            if (!testDAO.exists(query)) {
                updateJobStatus.set("state", State.DONE).set("endTime", new Date());
                updateBuild.inc("buildStatus.doneJobs").dec("buildStatus.runningJobs");
            }
            Job job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(result.getJobId()), updateJobStatus);

            if( logger.isDebugEnabled() ) {
                logger.debug(
                    "After modifying job ( after runningTests decrement ), runningTests:[{}]",
                    job.getRunningTests());
            }

            Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()), updateBuild);

            broadcastMessage(MODIFIED_BUILD, build);
            broadcastMessage(MODIFIED_TEST, result);
            broadcastMessage(MODIFIED_JOB, job);
            logger.info("succeed finish test- id:[{}], name:[{}]", result.getId(), result.getName());
            return result;
        } catch (Exception e) {
            logger.error("failed to finish test because: ", e);
            throw e;
        } finally {
            if (test.getAssignedAgent() != null) {
                UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date())
                        .removeAll("currentTests", test.getId());
                Agent agent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(test.getAssignedAgent()), updateOps, false, false);
                if (agent != null) {
                Agent idling = null;
                    if (agent.getCurrentTests().isEmpty()) {
                        idling = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(test.getAssignedAgent())
                                        .where("this.currentTests.length == 0"),
                                agentDAO.createUpdateOperations().set("state", Agent.State.IDLING));
                        if (idling != null) {
                            logger.debug("agent [{}] become idling because it finish all his tests", idling.getName());
                        }
                    }
                    broadcastMessage(MODIFIED_AGENT, idling == null ? agent : idling);
                }
            }
        }
    }


    @GET
    @Path("job-tests-view")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<TestView> getJobTestsView(@DefaultValue("0") @QueryParam("offset") int offset,
                                           @DefaultValue("30") @QueryParam("limit") int limit,
                                           @DefaultValue("false") @QueryParam("all") boolean all,
                                           @QueryParam("orderBy") List<String> orderBy,
                                           @QueryParam("jobId") String jobId,
                                           @Context UriInfo uriInfo) {

        List<Test> tests = retrieveJobTests(jobId, orderBy, all, offset, limit);
        List<TestView> testsView = new ArrayList<>(tests.size());
        for (Test test : tests) {
            testsView.add(new TestView(test));
        }

        return new Batch<>(testsView, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Test> getJobTests(@DefaultValue("0") @QueryParam("offset") int offset,
                                   @DefaultValue("30") @QueryParam("limit") int limit,
                                   @DefaultValue("false") @QueryParam("all") boolean all,
                                   @QueryParam("orderBy") List<String> orderBy,
                                   @QueryParam("jobId") String jobId,
                                   @Context UriInfo uriInfo) {

        List<Test> tests = jobId != null && jobId.trim().length() > 0 ?
                retrieveJobTests(jobId, orderBy, all, offset, limit) : Collections.emptyList();
        return new Batch<>(tests, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("agent-tests")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Test> getAgentTests(@DefaultValue("0") @QueryParam("offset") int offset,
                                     @DefaultValue("30") @QueryParam("limit") int limit,
                                     @DefaultValue("false") @QueryParam("all") boolean all,
                                     @QueryParam("orderBy") List<String> orderBy,
                                     @QueryParam("agentName") String agentName,
                                     @Context UriInfo uriInfo) {

        List<Test> tests = agentName != null && agentName.trim().length() > 0 ?
                retrieveAgentTests(agentName, orderBy, all, offset, limit) : Collections.emptyList();
        return new Batch<>(tests, offset, limit, all, orderBy, uriInfo);
    }

    private List<Test> retrieveJobTests(String jobId, List<String> orderBy, boolean all, int offset, int limit) {

        Query<Test> query = testDAO.createQuery();
        if (jobId != null) {
            query.field("jobId").equal(jobId);
        }

        return applySelectTestsPropertiesAndGet(query, orderBy, all, offset, limit, new String[]{"sha", "properties", "testType", "timeout", "logs", "scheduledAt"});
    }

    private List<Test> retrieveAgentTests(String agentName, List<String> orderBy, boolean all, int offset, int limit) {

        Query<Test> query = testDAO.createQuery();
        if (agentName != null) {
            query.field("assignedAgent").equal(agentName);
        }

        return applySelectTestsPropertiesAndGet(query, orderBy, all, offset, limit, new String[]{"sha", "properties", "assignedAgent", "testType", "timeout", "logs", "scheduledAt"});
    }

    private List<Test> applySelectTestsPropertiesAndGet(Query<Test> query, List<String> orderBy, boolean all, int offset, int limit, String[] strings) {

        if (orderBy != null) {
            if (orderBy.isEmpty()) {
                orderBy.add("startTime");
            }
            orderBy.forEach(query::order);
        }
        if (!all) {
            query.offset(offset).limit(limit);
        }
        //redundant test properties for job tests view
        query.retrievedFields(false, strings);
        return testDAO.find(query).asList();
    }

    @GET
    @Path("test/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("id") String id) {
        Test test = testDAO.findOne(testDAO.createQuery().field("_id").equal(new ObjectId(id)));
        return test;
    }

    @POST
    @Path("test/{jobId}/{id}/log")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Test uploadTestLog(FormDataMultiPart form,
                              @PathParam("jobId") String jobId,
                              @PathParam("id") String id,
                              @Context UriInfo uriInfo) {
        if (getJob(jobId) == null) {
            logger.warn("uploadTestLog - the job of the test is not on database. testId:[{}], jobId:[{}].", id, jobId);
            return null;
        }
        FormDataBodyPart filePart = form.getField("file");
        ContentDisposition contentDispositionHeader = filePart.getContentDisposition();
        InputStream fileInputStream = filePart.getValueAs(InputStream.class);
        String fileName = contentDispositionHeader.getFileName();

        if (fileName.toLowerCase().endsWith(".zip")) {
            handleTestLogBundle(id, jobId, uriInfo, fileInputStream, fileName);
        } else {
            handleTestLogFile(id, jobId, uriInfo, fileInputStream, fileName);
        }

        return null;
    }

    @POST
    @Path("job/{jobId}/{agentName}/log")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Job uploadJobSetupLog(FormDataMultiPart form,
                                 @PathParam("jobId") String jobId, @PathParam("agentName") String agentName,
                                 @Context UriInfo uriInfo) {
        if (getJob(jobId) == null) {
            logger.warn("uploadJobSetupLog - the job is not in the database. jobId:[{}].", jobId);
            return null;
        }
        if (getAgent(agentName) == null) {
            logger.warn("uploadJobSetupLog - the agent is not in the database. agent:[{}].", agentName);
            return null;
        }
        FormDataBodyPart filePart = form.getField("file");
        ContentDisposition contentDispositionHeader = filePart.getContentDisposition();
        InputStream fileInputStream = filePart.getValueAs(InputStream.class);
        String fileName = contentDispositionHeader.getFileName();
        handleJobSetupLogFile(jobId, agentName, uriInfo, fileInputStream, fileName);

        return null;
    }

    private void handleJobSetupLogFile(String jobId, String agentName, UriInfo uriInfo, InputStream fileInputStream, String fileName) {
        String filePath = calculateJobSetupLogFilePath(jobId, agentName) + fileName;
        try {
            saveFile(fileInputStream, filePath);
            URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();
            UpdateOperations<Job> updateOps = jobDAO.createUpdateOperations().set("jobSetupLogs." + getLogName(agentName.replace(".", "_")), uri.toASCIIString());
            Job job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId), updateOps);
            broadcastMessage(MODIFIED_JOB, job);
        } catch (IOException e) {
            logger.error("Failed to save log at {} for jobId {}", filePath, jobId, e);
        }
    }

    private void handleTestLogFile(String testId, String jobId, UriInfo uriInfo, InputStream fileInputStream, String fileName) {
        String filePath = calculateTestLogFilePath(jobId, testId) + fileName;
        try {
            saveFile(fileInputStream, filePath);
            URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();
            UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("logs." + getLogName(fileName), uri.toASCIIString());
            Test test = testDAO.getDatastore().findAndModify(testDAO.createIdQuery(testId), updateOps);
            broadcastMessage(MODIFIED_TEST, test);
        } catch (IOException e) {
            logger.error("Failed to save log at {} for test {} jobId {}", filePath, testId, jobId, e);
        }
    }

    private static String calculateTestLogFilePath(String jobId, String testId) {
        return SERVER_TESTS_UPLOAD_LOCATION_FOLDER + "/" + jobId + "/" + testId + "/";
    }

    private static String calculateJobSetupLogFilePath(String jobId, String agentName) {
        return SERVER_JOBS_UPLOAD_LOCATION_FOLDER + "/" + jobId + "/" + agentName + "/";
    }

    private void handleTestLogBundle(String testId, String jobId, UriInfo uriInfo, InputStream fileInputStream, String fileName) {

        String filePath = calculateTestLogFilePath(jobId, testId) + fileName;
        try {
            saveFile(fileInputStream, filePath);
            Set<String> entries = extractZipEntries(filePath);
            URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();
            UpdateOperations<Test> updateOps = testDAO.createUpdateOperations();
            for (String entry : entries) {
                updateOps.set("logs." + entry.replaceAll("\\.", "_"), uri.toASCIIString() + "!/" + entry);
                //https://localhost:8443/api/newman/test/1/logBundle/logs.zip!/logs/pom_files/microsoft.owa.extendedmaillistview.mouse.js
            }
            Test test = testDAO.getDatastore().findAndModify(testDAO.createIdQuery(testId), updateOps);
            broadcastMessage(MODIFIED_TEST, test);
        } catch (IOException e) {
            logger.error("Failed to save log at {} for test {} jobId {}", filePath, testId, jobId, e);
        }
    }


    private Set<String> extractZipEntries(String filePath) throws IOException {
        Set<String> res = new HashSet<>();
        ZipEntry zEntry;
        try (FileInputStream fis = new FileInputStream(filePath); ZipInputStream zipIs = new ZipInputStream(new BufferedInputStream(fis))) {
            while ((zEntry = zipIs.getNextEntry()) != null) {
                if (!zEntry.isDirectory()) {
                    res.add(zEntry.getName());
                }
            }
            zipIs.close();
        }
        return res;
    }

    @GET
    @Path("test/{jobId}/{id}/log/{name}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response downloadTestLog(@PathParam("jobId") String jobId, @PathParam("id") String id, @PathParam("name") String name,
                                    @DefaultValue("false") @QueryParam("download") boolean download) {
        MediaType mediaType;
        if (download) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }
        String filePath = calculateTestLogFilePath(jobId, id) + name;

        return Response.ok(new File(filePath), mediaType).build();
    }

    @GET
    @Path("job/{jobId}/{agentName}/log/{name}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response downloadJobSetupLog(@PathParam("jobId") String jobId, @PathParam("agentName") String agentName, @PathParam("name") String name,
                                        @DefaultValue("false") @QueryParam("download") boolean download) {
        MediaType mediaType;
        if (download) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }
        String filePath = calculateJobSetupLogFilePath(jobId, agentName) + name;

        return Response.ok(new File(filePath), mediaType).build();
    }

    @GET
    @Path("resource/{branch}/{buildName}/{resourceName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getBuildResource(@PathParam("branch") String branch,
                                     @PathParam("buildName") String buildName,
                                     @PathParam("resourceName") String resourceName) throws IOException {
        MediaType mediaType;
        mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

        String filePath = calcCacheResourceToDownload(branch, buildName, resourceName, true);
        InputStream is = getBufferedInputStream(filePath);
        return Response.ok(is, mediaType).build();
    }

    @GET
    @Path("metadata/{branch}/{buildName}/{resourceName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getBuildMetadata(@PathParam("branch") String branch,
                                     @PathParam("buildName") String buildName,
                                     @PathParam("resourceName") String resourceName) throws IOException {
        MediaType mediaType;
        mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

        String filePath = calcCacheResourceToDownload(branch, buildName, resourceName, false);
        InputStream is = getBufferedInputStream(filePath);
        return Response.ok(is, mediaType).build();
    }

    private BufferedInputStream getBufferedInputStream(String filePath) throws IOException {
        return new BufferedInputStream(org.apache.commons.io.FileUtils.openInputStream(new File(filePath)), 5 * 1024);
    }

    private String calcCacheResourceToDownload(String branch, String BuildName, String resourceName, boolean resource) {
        String resourcesOrMetadata = resource ? "resources" : "metadata";
        return SERVER_CACHE_BUILDS_FOLDER + "/" + branch + "/" + BuildName + "/" + resourcesOrMetadata + "/" + resourceName;
    }


    @GET
    @Path("cacheBuild")
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized Build cacheBuild(@QueryParam("buildIdToCache") String buildIdToCache) throws IOException, URISyntaxException {
        try {
            Build build = getBuild(buildIdToCache);

            if (build == null) {
                throw new IllegalArgumentException("cant cache the build with id " + buildIdToCache + " because it does not exists");
            }

            if (!buildCacheEnabled) {
                logger.info("build caching is disabled");
                return build;
            }

            if (isBuildInCache(build)) {
                logger.info("the build {} is already in cache", build);
                return build;
            }
            try {

                storeBuildInCache(build);

                downloadResourceAndMetadata(build);

                updateResourceAndMetadataURIs(build);
            } catch (Exception e) {
                // restore cache
                try {
                    removeFromCache(build);
                } finally {
                    FileUtils.delete(new File(calcCachePath(build)).toPath());
                }
                throw e;
            }

            logger.info("create cached build: [{}]", build);
            return build;
        } catch (Exception e) {
            logger.error("could not cache build", e);
            throw e;
        }
    }

    private boolean isBuildInCache(Build build) {
        return getCache().isInCache(build);
    }

    private void removeFromCache(Build build) {
        BuildsCache cache = getCache();
        cache.remove(build);
        updateBuildCache(cache);
    }

    private void updateBuildCache(BuildsCache cache) {
        UpdateOperations<BuildsCache> updateOps = buildsCacheDAO.createUpdateOperations();
        updateOps.set("cache", cache.getCache());
        updateOps.set("index", cache.getIndex());
        updateOps.set("size", cache.getSize());
        Query<BuildsCache> query = buildsCacheDAO.createIdQuery(cache.getId());
        buildsCacheDAO.getDatastore().findAndModify(query, updateOps);
    }

    private void storeBuildInCache(Build build) throws IOException {
        BuildsCache cache = getCache();
        Build toRemove = cache.put(build);
        updateBuildCache(cache);
        // evict if no room is left in the cache - revert resources links and delete artifacts from disk
        if (toRemove != null) {
            revertBuild(toRemove);
        }
    }

    private void revertBuild(Build toRemove) throws IOException {
        try {
            updateBuild(toRemove.getId(), toRemove);
        } finally {
            FileUtils.delete(new File(calcCachePath(toRemove)).toPath());
        }
    }

    private BuildsCache getCache() {
        BuildsCache cache = buildsCacheDAO.findOne(buildsCacheDAO.createQuery());
        if (cache == null) {
            throw new IllegalStateException("builds cache is null");
        }
        return cache;
    }

    @SuppressWarnings("unchecked")
    private void updateResourceAndMetadataURIs(Build build) throws URISyntaxException {
        String rootCacheFolder = calcCachePath(build);
        java.nio.file.Path pathToResources = FileUtils.append(rootCacheFolder, "resources");
        java.nio.file.Path pathToMetadata = FileUtils.append(rootCacheFolder, "metadata");
        final Collection resources = FileUtils.listFilesInFolder(pathToResources.toFile());
        final Collection metadata = FileUtils.listFilesInFolder(pathToMetadata.toFile());
        injectURIs(resources, true, build);
        injectURIs(metadata, false, build);
        updateBuild(build.getId(), build);
    }

    private void injectURIs(Collection<File> files, boolean isResource, Build build) {
        String branch = build.getBranch();
        String buildName = build.getName();
        String resourcesOrMetadata = isResource ? "resource" : "metadata";
        String uriPrefix = HTTP_WEB_ROOT_PATH + "/" + resourcesOrMetadata + "/" + branch + "/" + buildName + "/";
        Collection<URI> newResources = new ArrayList<>();
        for (File file : files) {
            newResources.add(URI.create(uriPrefix + file.getName()));
        }
        if (isResource) {
            build.setResources(newResources);
        } else {
            build.setTestsMetadata(newResources);
        }
    }

    private void downloadResourceAndMetadata(Build build) throws IOException {

        String pathToCache = calcCachePath(build);
        // create folders to store resources and metadata
        java.nio.file.Path rootCacheFolder = Paths.get(pathToCache);
        FileUtils.createFolder(rootCacheFolder);
        java.nio.file.Path pathToResources = FileUtils.append(rootCacheFolder, "resources");
        FileUtils.createFolder(pathToResources);
        java.nio.file.Path pathToMetadata = FileUtils.append(rootCacheFolder, "metadata");
        FileUtils.createFolder(pathToMetadata);
        // validate links to resources and metadata before downloading
        FileUtils.validateUris(build.getResources());
        FileUtils.validateUris(build.getTestsMetadata());
        // download resources and metadata to server cache
        logger.info("Downloading {} resources into {}...", build.getResources().size(), pathToResources);
        for (URI uri : build.getResources()) {
            logger.info("Downloading {}...", uri);
            try {
                FileUtils.download(uri.toURL(), pathToResources);
            } catch (IOException e) {
                logger.error("failed to download url[" + uri + "] to server", e);
                throw e;
            }
        }
        logger.info("Downloading {} metadata into {}...", build.getResources().size(), pathToMetadata);
        for (URI uri : build.getTestsMetadata()) {
            logger.info("Downloading {}...", uri);
            try {
                FileUtils.download(uri.toURL(), pathToMetadata);
            } catch (IOException e) {
                logger.error("failed to download test metadata url[" + uri + "] to server", e);
                throw e;
            }
        }
    }

    private String calcCachePath(Build build) {
        return SERVER_CACHE_BUILDS_FOLDER + "/" + build.getBranch() + "/" + build.getName() + "/";
    }


    @GET
    @Path("log/size")
    public Response computeLogDirSize() {
        synchronized (lastLogSizeCheckTime) {
            if (System.currentTimeMillis() - lastLogSizeCheckTime.get() < TimeUnit.MINUTES.toMillis(60)) {
                return Response.ok(String.valueOf(latestLogSize)).build();
            }
            lastLogSizeCheckTime.set(System.currentTimeMillis());
        }
        try {
            if (!new File(SERVER_TESTS_UPLOAD_LOCATION_FOLDER).exists() || !new File(SERVER_JOBS_UPLOAD_LOCATION_FOLDER).exists()) {
                return Response.ok("0").build();
            }
            long testLogsSize = Files.walk(Paths.get(SERVER_TESTS_UPLOAD_LOCATION_FOLDER)).mapToLong(p -> p.toFile().length()).sum();
            long jobsSetupLogSize = Files.walk(Paths.get(SERVER_JOBS_UPLOAD_LOCATION_FOLDER)).mapToLong(p -> p.toFile().length()).sum();
            latestLogSize.set(testLogsSize + jobsSetupLogSize);
            String sum = String.valueOf(latestLogSize.get());
            return Response.ok(sum, MediaType.TEXT_PLAIN_TYPE).build();
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }
    }

    @GET
    @Path("agents/count")
    public Response getAgentsCount() {
        long count = agentDAO.count();
        if( logger.isDebugEnabled() ) {
            logger.debug("agents count=" + count);
        }
        return Response.ok(count, MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("agents/failing")
    public Response getFailingAgents() {
        long count = agentDAO.createQuery().filter("setupRetries >", 0).countAll();
        if( logger.isDebugEnabled() ) {
            logger.debug("agents failed setup count=" + count);
        }
        return Response.ok(count, MediaType.TEXT_PLAIN_TYPE).build();
    }


    @DELETE
    @Path("log")
    @RolesAllowed("admin")
    public Response deleteLogs() {
        try {
            java.nio.file.Path path = Paths.get(SERVER_TESTS_UPLOAD_LOCATION_FOLDER);
            FileUtils.delete(path);
            FileUtils.createFolder(path);
            return Response.ok(String.valueOf(Files.walk(path).mapToLong(p -> p.toFile().length()).sum()), MediaType.TEXT_PLAIN_TYPE).build();
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }
    }

    @GET
    @Path("test/{jobId}/{id}/log/{name:.*\\.zip!/.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response downloadCompressLog(@PathParam("jobId") String jobId, @PathParam("id") String id,
                                        @PathParam("name") String name,
                                        @DefaultValue("false") @QueryParam("download") boolean download) {
        try {
            MediaType mediaType;
            if (download) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            } else {
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            }
            //noinspection SpellCheckingInspection
            String[] splited = name.split("!");
            String zipPath = splited[0];
            String entryName = splited[1].substring(1);
            String filePath = calculateTestLogFilePath(jobId, id) + zipPath;
            ZipFile zip = new ZipFile(filePath);
            InputStream is = zip.getInputStream(zip.getEntry(entryName));
            return Response.ok(is, mediaType).build();
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }
    }

    private String getLogName(String fileName) {
        return fileName.replaceAll("\\.[^.]*$", "");
    }


    @GET
    @Path("subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Agent> getSubscriptions(@DefaultValue("0") @QueryParam("offset") int offset,
                                         @DefaultValue("30") @QueryParam("limit") int limit
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {
        Query<Agent> query = agentDAO.createQuery().offset(offset).limit(limit);
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }
        return new Batch<>(agentDAO.find(query).asList(), offset, limit,
                false, orderBy, uriInfo);
    }

    @GET
    @Path("ping/{name}/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String pingInSetup(@PathParam("name") final String name, @PathParam("jobId") final String jobId) {
        UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date());
        Agent agent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(name), updateOps, false, false);
        if (agent == null) {
            logger.error("Unknown agent " + name);
            return null;
        }
        String agentIp = agent.getHostAddress();
        if (offlineAgents.containsKey(agentIp)) {
            offlineAgents.remove(agentIp);
            broadcastMessage(DELETED_OFFLINE_AGENT, agentIp);
        }
        if (agent.getJobId() == null) {
            logger.error("Agent {} not working on job {} test {}", agent, jobId);
            return null;
        } else {
            broadcastMessage(MODIFIED_AGENT, agent);

            return agent.getJobId();
        }
    }

    @GET
    @Path("agent/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Agent getAgent(final @PathParam("name") String name) {
        Agent agent = agentDAO.findOne(agentDAO.createQuery().field("name").equal(name));
        //if such agent not found (already does not exist), then create Agent instance and
        //provide only its name
        if (agent == null) {
            agent = new Agent();
            agent.setName(name);
        }
        return agent;
    }

    @GET
    @Path("agent")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Agent> getAgents(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("30") @QueryParam("limit") int limit,
                                  @DefaultValue("false") @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {
        Query<Agent> query = agentDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }

        List<Agent> agents = agentDAO.find(query).asList();
        for (Agent agent : agents) {
            String jobId = agent.getJobId();
            if (jobId != null) {
                Job job = findOneThinJobById(jobId);
                if( logger.isDebugEnabled() ) {
                    logger.debug("within for on agents, jobID=" + jobId + ", job=" + job);
                }
                agent.setJob(job);
            }
        }

        return new Batch<>(agents, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("agent/offline")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Agent> getOfflineAgents() {

        List<Agent> result = new ArrayList<>(offlineAgents.size());
        for (Map.Entry<String,OfflineAgent> entry : offlineAgents.entrySet()) {
            Agent agent = createAgentFromOfflineAgent(entry.getValue());
            result.add(agent);
        }
        return result;
    }

    private Agent createAgentFromOfflineAgent(OfflineAgent offlineAgent) {
        Agent result = new Agent();
        result.setName(offlineAgent.getName());
        result.setHost(offlineAgent.getHost());
        result.setLastTouchTime(offlineAgent.getLastTouchTime());
        result.setId("");
        result.setJobId("");
        result.setHostAddress(offlineAgent.getHostAddress());
        result.setPid("");
        result.setJob(new Job());
        result.setState(Agent.State.IDLING);
        return result;
    }

    @POST
    @Path("agent/clean-setup-retries")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Agent> cleanAgentRetries(@DefaultValue("0") @QueryParam("offset") int offset,
                                          @DefaultValue("30") @QueryParam("limit") int limit,
                                          @DefaultValue("false") @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {
        Query<Agent> query = agentDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }

        UpdateOperations<Agent> updateAgentStatus = agentDAO.createUpdateOperations().set("setupRetries",0);
        UpdateResults update = agentDAO.getDatastore().update(query, updateAgentStatus);
        logger.info("cleanAgentRetries: number of agents updated " + update.getUpdatedCount());

        List<Agent> agents = agentDAO.find(query).asList();
        int count = 0;
        for (Agent agent : agents) {
            if (agent.getSetupRetries() > 0) {
                count++;
            }
            broadcastMessage(MODIFIED_AGENT, agent);
        }
        broadcastMessage(MODIFIED_FAILING_AGENTS, count);
        return new Batch<>(agents, offset, limit, all, orderBy, uriInfo);
    }

    @POST
    @Path("agent/{name}/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("name") final String name, @PathParam("jobId") final String jobId) {
        checkServerStatus();

        Agent agent = agentDAO.findOne(agentDAO.createQuery().field("name").equal(name));
        if (agent == null) {
            logger.error("bad request unknown agent {}", name);
            return null;
        }
        if (!jobId.equals(agent.getJobId())) {
            logger.error("Agent agent is not on job {} {} ", jobId, agent);
            return null;
        }


        Job pj = null;
        final Object lock = getAgentLock(agent);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            if (agent.getState() == Agent.State.PREPARING) {
                pj = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId), jobDAO.createUpdateOperations().removeAll("preparingAgents", agent.getName()));
            }
        }

        UpdateOperations<Agent> agentUpdateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date());
        Test result;
        UpdateOperations<Test> updateOps;
        Query<Test> query;
        synchronized (takenTestLock) {
            query = testDAO.createQuery();
            query.and(query.criteria("jobId").equal(jobId), query.criteria("status").equal(Test.Status.PENDING));
            updateOps = testDAO.createUpdateOperations().set("status", Test.Status.RUNNING)
                    .set("assignedAgent", name).set("startTime", new Date());
            result = testDAO.getDatastore().findAndModify(query, updateOps, false, false);
        }

        if (result != null) {
            UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().inc("runningTests");
            Job job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId).field("state").notEqual(State.PAUSED), updateJobStatus);
            if (job != null) {
                if( logger.isDebugEnabled() ) {
                    logger.debug("After incrementing runningTests for jobId [{}] runningTests [{}]",
                                 jobId, job.getRunningTests());
                }
                UpdateOperations<Build> buildUpdateOperations = buildDAO.createUpdateOperations().inc("buildStatus.runningTests");
                UpdateOperations<Job> jobUpdateOperations = jobDAO.createUpdateOperations();
                jobUpdateOperations.set("state", State.RUNNING);
                if (job.getStartTime() == null) {
                    jobUpdateOperations.set("startTime", new Date());
                    buildUpdateOperations.inc("buildStatus.runningJobs").dec("buildStatus.pendingJobs");
                }
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId).field("state").notEqual(State.PAUSED), jobUpdateOperations);
                agentUpdateOps.add("currentTests", result.getId());
                agentUpdateOps.set("state", Agent.State.RUNNING);
                if (job != null) {
                    Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()),
                            buildUpdateOperations);
                    broadcastMessage(MODIFIED_JOB, job);
                    if (build != null) {
                        broadcastMessage(MODIFIED_BUILD, build);
                    }
                }
                broadcastMessage(MODIFIED_TEST, result);
            } else {
                // return the test to the pool.
                updateOps = testDAO.createUpdateOperations().set("status", Test.Status.PENDING)
                        .unset("assignedAgent").unset("startTime");
                Test test = testDAO.getDatastore().findAndModify(query, updateOps, false, false);
                broadcastMessage(MODIFIED_TEST, test);

                agent = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()),
                        agentDAO.createUpdateOperations().removeAll("currentTests", result.getId()), false, true);
                if (agent.getCurrentTests().isEmpty()) {
                    Agent idling = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()).where("this.currentTests.length == 0"),
                            agentDAO.createUpdateOperations().set("state", Agent.State.IDLING));
                    if (idling != null) {
                        agent = idling;
                        logger.warn("agent [{}] is idling from getTest because finished all his tests", agent.getName());
                    }
                }

                broadcastMessage(MODIFIED_AGENT, agent);
                if (pj != null) {
                    broadcastMessage(MODIFIED_JOB, pj);
                }
                return null;
            }
        }
        agent = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()), agentUpdateOps, false, true);
        broadcastMessage(MODIFIED_AGENT, agent);
        logger.warn("agent [{}] got test id: [{}], test-state:[{}]", agent.getName(), result.getId(), result.getStatus());
        return result;
    }

    private Object getAgentLock(Agent agent) {
        agentLocks.putIfAbsent(agent.getName(), new Object());
        return agentLocks.get(agent.getName());
    }

    @POST
    @Path("subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response subscribe(final Agent agent) {
        checkServerStatus();

        agentLocks.putIfAbsent(agent.getName(), new Object());
        Agent found = agentDAO.findOne("name", agent.getName());
        if (found != null) {
            // clear older agent data from other jobs.
            if (found.getState() == Agent.State.PREPARING) {
                // clear job data if exists.
                if (found.getJobId() != null && !found.getJobId().isEmpty()) {
                    UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().removeAll("preparingAgents", agent.getName());
                    Job oldJob = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(found.getJobId()), updateJobStatus);
                    broadcastMessage(MODIFIED_JOB, oldJob);
                }
            } else if (found.getState() == Agent.State.RUNNING && !found.getCurrentTests().isEmpty() && found.getJobId() != null) {
                returnTests(found);
            }
        }

        Query<Job> basicQuery = basicJobQuery();
        //Return jobs that need more agents that it has now
        basicQuery.or(
                basicQuery.and(basicQuery.criteria("preparingAgents").exists(),
                        new WhereCriteria("this.preparingAgents.length < (this.totalTests + this.numOfTestRetries - this.passedTests - this.failedTests - this.runningTests)")),
                basicQuery.criteria("preparingAgents").doesNotExist()
        );

        Job job = findJob(agent.getCapabilities(), basicQuery);

        UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations()
                .set("lastTouchTime", new Date());
        if (agent.getHost() != null) {
            updateOps.set("host", agent.getHost());
            updateOps.set("hostAddress", agent.getHostAddress());
            updateOps.set("pid", agent.getPid());
            updateOps.set("capabilities", agent.getCapabilities());
        }
        updateOps.set("currentTests", new HashSet<String>());
        if (job != null) {
            updateOps.set("jobId", job.getId());
            updateOps.set("state", Agent.State.PREPARING);
            UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().add("preparingAgents", agent.getName());
            // update startPrepareTime only if not set
            if (job.getStartPrepareTime() == null) {
                logger.info("agent [host:[{}], name:[{}]] start prepare on job [id:[{}], name:[{}]].", agent.getHost(), agent.getName(), job.getId(), job.getSuite().getName());
                updateJobStatus.set("startPrepareTime", new Date());
            }
            // job can be run = not a zombie
            if (job.getLastTimeZombie() != null) {
                updateJobStatus.unset("lastTimeZombie");
            }
            job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(job.getId()), updateJobStatus);
            broadcastMessage(MODIFIED_JOB, job);
        } else {
            updateOps.set("state", Agent.State.IDLING);
        }

        Agent readyAgent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(agent.getName()), updateOps, false, true);
        if (found == null) {
            String agentIp = agent.getHostAddress();
            if (offlineAgents.containsKey(agentIp)) {
                offlineAgents.remove(agentIp);
                broadcastMessage(DELETED_OFFLINE_AGENT, agentIp);
            }
            broadcastMessage(MODIFIED_AGENTS_COUNT, agentDAO.count());
        }
        if (readyAgent != null && readyAgent.getState().equals(Agent.State.IDLING)) {
            logger.debug("agent [{}] is idling at subscribe because didn't find job", readyAgent.getName());
        }

        //TODO this event is sent every 10 seconds (jobPollInteval) per agent
        //TODO Check if it is necessary to send it
        //logger.info(">>> Modified Agent " +agent.getHost()+ " subscribe");
        broadcastMessage(MODIFIED_AGENT, readyAgent);
        return Response.ok(job).build();
    }

    private Job findJob(Set<String> capabilities, Query<Job> basicQuery) {
        List<Job> jobs = null;
        Job job = null;
        if (!capabilities.isEmpty()) { // if agent has capabilities
            Query<Job> requirementsQuery = basicQuery.cloneQuery();
            jobs = requirementsQuery.field("suite.requirements").in(capabilities).asList();
        }
        if (jobs != null && jobs.size() > 0) { // if found jobs with match requirements
            List<Job> jobsFilterByCapabilities = CapabilitiesAndRequirements.filterByCapabilities(jobs, capabilities); // filter jobs with not supported requirements
            job = jobsFilterByCapabilities.get(0);
        }
        if (job == null) { // search for jobs without requirements
            Query<Job> noRequirementsQuery = basicQuery.cloneQuery();
            noRequirementsQuery.field("suite.requirements").doesNotExist();
            job = jobDAO.findOne(noRequirementsQuery);
        }
        return job;
    }

    private Build getLatestBuild(String branch) {
        return buildDAO.findOne(buildDAO.createQuery().order("-buildTime").field("branch").equal(branch));
    }


    @GET
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<Build> getBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("50") @QueryParam("limit") int limit,
                                  @DefaultValue("false") @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {
        Query<Build> query = buildDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }
        return new Batch<>(buildDAO.find(query).asList(), offset, limit, all, orderBy, uriInfo);
    }


    @GET
    @Path("latest-builds")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Build> getLatestBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                        @DefaultValue("50") @QueryParam("limit") int limit,
                                        @DefaultValue("false") @QueryParam("all") boolean all,
                                        @QueryParam("branch") String branchStr,
                                        @QueryParam("tags") String tagsStr,
                                        @DefaultValue("true")  @QueryParam("with-all-jobs-completed") boolean withAllJobsCompleted,
                                        @Context UriInfo uriInfo) {

        Query<Build> query = buildDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }

        List<String> orderBy = new ArrayList<>(1);
        orderBy.add("-buildTime");
        orderBy.forEach(query::order);

        Set<String> tagsSet;
        if (tagsStr != null && !tagsStr.isEmpty()) {
            tagsSet = new HashSet<>(Arrays.asList(tagsStr.split("\\s*,\\s*")));
            if (!tagsSet.isEmpty()) { // build should be with specifics tags
                query.field("tags").hasAllOf(tagsSet);
            }
        }

        if( branchStr != null && !branchStr.isEmpty() ) {
            query.field("branch").equal(branchStr);
        }

        if( withAllJobsCompleted ) {
            query = query
                .where("this.buildStatus.totalJobs>0")
                .where(
                    "this.buildStatus.doneJobs + this.buildStatus.brokenJobs == this.buildStatus.totalJobs");
        }

        return new Batch<>(buildDAO.find(query).asList(), offset, limit, all, orderBy, uriInfo);
    }

    @PUT
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build createBuild(final Build build) {
        if (build.getBuildTime() == null) {
            build.setBuildTime(new Date());
        }
        build.setBuildStatus(new BuildStatus());
        buildDAO.save(build);
        broadcastMessage(CREATED_BUILD, build);
        return build;
    }

    @POST
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build appendToBuild(final Build build) {

        if (build.getName() == null) {
            throw new BadRequestException("can't append to build " + build + " without a name");
        }

        Build found = buildDAO.findOne(buildDAO.createQuery().field("name").equal(build.getName()));
        if (found == null) {
            throw new BadRequestException("can't append to build " + build + " since it does not exists");
        }

        if (build.getId() != null && !found.getId().equalsIgnoreCase(build.getId())) {
            throw new UnsupportedOperationException("appending build id is not supported");
        }

        if (build.getBranch() != null && !build.getBranch().equalsIgnoreCase(found.getBranch())) {
            throw new UnsupportedOperationException("appending branch is not supported");
        }

        if (build.getShas() != null && !build.getShas().isEmpty()) {
            found.getShas().putAll(build.getShas());
        }

        if (build.getResources() != null && !build.getResources().isEmpty()) {
            found.getResources().addAll(build.getResources());
        }

        if (build.getTestsMetadata() != null && !build.getTestsMetadata().isEmpty()) {
            found.getTestsMetadata().addAll(build.getTestsMetadata());
        }

        if (build.getTags() != null && !build.getTags().isEmpty()) {
            found.getTags().addAll(build.getTags());
        }

        return updateBuild(found.getId(), found);
    }

    @POST
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build updateBuild(final @PathParam("id") String id, final Build build) {
        UpdateOperations<Build> updateOps = buildDAO.createUpdateOperations();
        if (build.getShas() != null) {
            //noinspection SpellCheckingInspection
            updateOps.set("shas", build.getShas());
        }
        if (build.getBranch() != null) {
            updateOps.set("branch", build.getBranch());
        }
        if (build.getResources() != null) {
            updateOps.set("resources", build.getResources());
        }
        if (build.getTestsMetadata() != null) {
            updateOps.set("testsMetadata", build.getTestsMetadata());
        }
        if (build.getTags() != null) {
            updateOps.set("tags", build.getTags());
        }
        Query<Build> query = buildDAO.createIdQuery(id);
        Build result = buildDAO.getDatastore().findAndModify(query, updateOps);
        if (result != null) {
            broadcastMessage(MODIFIED_BUILD, result);
        }
        return result;
    }

    @POST
    @Path("job/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job updateJob(final @PathParam("id") String id, final Job job) {
        UpdateOperations<Job> updateOps = jobDAO.createUpdateOperations();
        if (job.getState() != null) {
            updateOps.set("state", job.getState());
        }
        Query<Job> query = jobDAO.createIdQuery(id);
        Job result = jobDAO.getDatastore().findAndModify(query, updateOps);
        if (result != null) {
            broadcastMessage(MODIFIED_JOB, result);
        }
        return result;
    }

    @POST
    @Path("update-suite")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Suite updateSuite(Suite suite) {

        logger.info("---updateSuite()");

        if (suite.getDisplayedCriteria() != null) {
            try {
                Criteria criteria = new ObjectMapper().readValue(suite.getDisplayedCriteria(), Criteria.class);
                suite.setCriteria(criteria);
            } catch (IOException e) {
                logger.error(e.toString(), e);
            }
        }

        UpdateOperations<Suite> updateOps = suiteDAO.createUpdateOperations();
        if (suite.getCriteria() != null) {
            updateOps.set(CRITERIA_PROP_NAME, suite.getCriteria());
        }
        if (suite.getCustomVariables() != null) {
            updateOps.set("customVariables", suite.getCustomVariables());
        }
        String id = suite.getId();
        Query<Suite> query = suiteDAO.createIdQuery(id);
        Suite result = suiteDAO.getDatastore().findAndModify(query, updateOps);
        if (result != null) {
            broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(result));
        }

        return suite;
    }


    @GET
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Build getBuild(final @PathParam("id") String id) {
        Build build = buildDAO.findOne(buildDAO.createIdQuery(id));
        logger.info("Build [" + build.getName() + "] [" + build.getId() + "] shas:" + Arrays.toString(build.getShas().entrySet().toArray(new Map.Entry[build.getShas().size()])));
        return build;
    }


    @DELETE
    @Path("db")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCollections() {
        MongoDatabase db = mongoClient.getDatabase(config.getMongo().getDb());
        List<String> deleted = new ArrayList<>();
        for (String name : db.listCollectionNames()) {
            if (!"system.indexes".equals(name)) {
                MongoCollection myCollection = db.getCollection(name);
                myCollection.drop();
                deleted.add(name);
            }
        }
        return Response.ok(Entity.json(deleted)).build();
    }

    @DELETE
    @Path("job/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJob(final @PathParam("jobId") String jobId) {
        Job deletedJob = performDeleteJob(jobId);
        performDeleteTestsLogs(jobId);
        performDeleteJobSetupLogs(jobId);
        updateBuildWithDeletedJob(deletedJob);
        performDeleteTests(jobId);
        return Response.ok(Entity.json(jobId)).build();
    }

    private void performDeleteTestsLogs(String jobId) {
        java.nio.file.Path path = Paths.get(SERVER_TESTS_UPLOAD_LOCATION_FOLDER + "/" + jobId);
        try {
            FileUtils.delete(path);
            logger.info("Log file {} was deleted", path);
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
    }

    private void performDeleteJobSetupLogs(String jobId) {
        java.nio.file.Path path = Paths.get(SERVER_JOBS_UPLOAD_LOCATION_FOLDER + "/" + jobId);
        try {
            FileUtils.delete(path);
            logger.info("Log file {} was deleted", path);
        } catch (IOException e) {
            logger.error(e.toString(), e);
        }
    }

    private void updateBuildWithDeletedJob(Job deletedJob) {
        Query<Build> query = buildDAO.createIdQuery(deletedJob.getBuild().getId());
        Build associatedBuild = buildDAO.findOne(query);
        if (associatedBuild != null) {
            BuildStatus associatedBuildStatus = associatedBuild.getBuildStatus();
            State state = deletedJob.getState();

            UpdateOperations<Build> updateOps = buildDAO.createUpdateOperations();

            if (state == State.DONE) {
                int curDoneJobs = associatedBuildStatus.getDoneJobs();
                if (curDoneJobs > 0) {
                    updateOps.set("buildStatus.doneJobs", curDoneJobs - 1);
                }
            } else if (state == State.PAUSED) {
                int curPendingJobs = associatedBuildStatus.getPendingJobs();
                if (curPendingJobs > 0) {
                    updateOps.set("buildStatus.pendingJobs", curPendingJobs - 1);
                }
            } else if (state == State.BROKEN) {
                int currBrokenJobs = associatedBuildStatus.getBrokenJobs();
                if (currBrokenJobs > 0) {
                    updateOps.set("buildStatus.brokenJobs", currBrokenJobs - 1);
                }
            }

            updateOps.set("buildStatus.totalJobs", associatedBuildStatus.getTotalJobs() - 1);

            Suite suite = deletedJob.getSuite();
            if (suite != null) {
                updateOps.removeAll("buildStatus.suitesNames", suite.getName());
                updateOps.removeAll("buildStatus.suitesIds", suite.getId());
            }

            Build modifiedBuild = buildDAO.getDatastore().findAndModify(query, updateOps);
            broadcastMessage(MODIFIED_BUILD, modifiedBuild);
        }
    }

    private Job performDeleteJob(String jobId) {
        Query<Job> idJobQuery = jobDAO.createIdQuery(jobId);
        Datastore datastore = jobDAO.getDatastore();
        return datastore.findAndDelete(idJobQuery);
    }

    private void performDeleteTests(String jobId) {
        Query<Test> testQuery = testDAO.createQuery();
        testQuery.and(testQuery.criteria("jobId").equal(jobId));
        Datastore datastore = testDAO.getDatastore();
        datastore.delete(testQuery);
    }

    @DELETE
    @Path("agent/{agentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAgent(final @PathParam("agentId") String agentId) {

        Query<Agent> idAgentQuery = agentDAO.createIdQuery(agentId);
        Datastore datastore = agentDAO.getDatastore();
        datastore.findAndDelete(idAgentQuery);
        broadcastMessage(MODIFIED_AGENTS_COUNT, agentDAO.count());
        return Response.ok(Entity.json(agentId)).build();
    }

    @DELETE
    @Path("offlineAgent/{agentName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOfflineAgent(final @PathParam("agentName") String agentName) {

        offlineAgents.remove(agentName);
        return Response.ok(Entity.json(agentName)).build();
    }

    @DELETE
    @Path("db/{collectionName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCollection(final @PathParam("collectionName") String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(config.getMongo().getDb());
        MongoCollection myCollection = db.getCollection(collectionName);
        if (myCollection != null) {
            myCollection.drop();
            return Response.ok(Entity.json(collectionName)).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    public UserPrefs getCurrentUser(@Context SecurityContext sc) {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setUserName(sc.getUserPrincipal().getName());
        return userPrefs;
    }

    @GET
    @Path("db")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollections() {
        MongoDatabase db = mongoClient.getDatabase(config.getMongo().getDb());
        List<String> res = new ArrayList<>();
        for (String name : db.listCollectionNames()) {
            if (!"system.indexes".equals(name)) {
                res.add(name);
            }
        }
        return Response.ok(Entity.json(res)).build();
    }

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDb() throws JsonProcessingException {
        return Response.ok(Entity.json(config.asJSON())).build();
    }

    @POST
    @Path("suite/{sourceSuiteId}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Suite addSuite(final @PathParam("sourceSuiteId") String sourceSuiteId, final @PathParam("name") String name) {

        Suite sourceSuite = getSuite(sourceSuiteId);
        Suite duplicatedSuite = sourceSuite;
        duplicatedSuite.setName(name);
        duplicatedSuite.setId(null);

        suiteDAO.save(duplicatedSuite);
        logger.info("---addSuite---" + duplicatedSuite);
        broadcastMessage(CREATED_SUITE, new SuiteWithJobs(duplicatedSuite));
        return duplicatedSuite;
    }

    @POST
    @Path("suite")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Suite addSuite(Suite suite) {
        suiteDAO.save(suite);
        logger.info("---addSuite---" + suite);
        broadcastMessage(CREATED_SUITE, new SuiteWithJobs(suite));
        return suite;
    }

    @GET
    @Path("suite/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Suite getSuite(final @PathParam("id") String id) {
        return suiteDAO.findOne(suiteDAO.createIdQuery(id));
    }

    @GET
    @Path("suite")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<SuiteView> getAllSuites(@DefaultValue("0") @QueryParam("offset") int offset,
                                         @DefaultValue("30") @QueryParam("limit") int limit
            , @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

        List<Suite> suites = getAllThinSuites(true);
        List<SuiteView> suiteViews = new ArrayList<>(suites.size());
        for (Suite suite : suites) {
            suiteViews.add(new SuiteView(suite));
        }

        return new Batch<>(suiteViews, offset, limit, all, orderBy, uriInfo);
    }

    @POST
    @Path("job-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JobConfig addJobConfig(JobConfig jobConfig) {
        jobConfigDAO.save(jobConfig);
        logger.info("---addJobConfig---" + jobConfig);
        broadcastMessage(CREATED_JOB_CONFIG, jobConfig);
        return jobConfig;
    }

    @GET
    @Path("job-config-by-id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobConfig getJobConfigById(final @PathParam("id") String id) {
        JobConfig jobConfig = jobConfigDAO.findOne(jobConfigDAO.createIdQuery(id));
        return jobConfig;
    }

    @GET
    @Path("job-config/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobConfig getJobConfigByName(final @PathParam("name") String name) {
        JobConfig jobConfig = jobConfigDAO.findOne(jobConfigDAO.createQuery().field("name").equal(name));
        return jobConfig;
    }

    @GET
    @Path("job-config")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobConfig> getAllJobConfig() {

        List<JobConfig> jobConfigs = jobConfigDAO.find(jobConfigDAO.createQuery()).asList();

        return jobConfigs;
    }

    @POST
    @Path("job-config-from-gui")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JobConfig addJobConfig(@QueryParam("name") String name,@QueryParam("javaVersion") String javaVersion) {
        JobConfig jobConfig = new JobConfig();
        jobConfig.setJavaVersion(JavaVersion.valueOf(javaVersion));
        jobConfig.setName(name);

        jobConfigDAO.save(jobConfig);
        broadcastMessage(CREATED_JOB_CONFIG, jobConfig);
        return jobConfig;
    }

    @GET
    @Path("java-versions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JavaVersion> getAllJavaVersions() {

        List<JavaVersion> javaVersions = Arrays.asList( JavaVersion.values());

        return javaVersions;
    }

    /**
     * With only if and name
     *
     * @return
     */
    private List<Suite> getAllThinSuites(boolean withCustomVariables) {

        Query<Suite> suitesQuery = suiteDAO.createQuery();
        if (withCustomVariables) {
            suitesQuery.retrievedFields(true, "id", "name", "customVariables");
        } else {
            suitesQuery.retrievedFields(true, "id", "name");
        }
        suitesQuery.order("name");

        List<Suite> suites = suiteDAO.find(suitesQuery).asList();
        return suites;
    }

    @GET
    @Path("suites-dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSuitesWithJobs() {

        List<Suite> suites = getAllThinSuites(false);//suiteDAO.find(suitesQuery).asList();

        List<SuiteWithJobs> suitesWithJobs = new ArrayList<>(suites.size());
        suitesWithJobs.addAll(suites.stream().map(this::createSuiteWithJobs).collect(Collectors.toList()));

        //reverse map in order to display first latest suites
        Collections.reverse(suitesWithJobs);

        return Response.ok(Entity.json(suitesWithJobs)).build();
    }

    @GET
    @Path("test-history")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<TestHistoryItem> getTests(@QueryParam("id") String id,
                                           @DefaultValue("0") @QueryParam("offset") int offset,
                                           @DefaultValue("50") @QueryParam("limit") int limit, @Context UriInfo uriInfo) {

        Test thisTest = getTest(id);

        String testName = thisTest.getName();
        Date endTime = thisTest.getEndTime();
        List<String> testArguments = thisTest.getArguments();

        String jobId = thisTest.getJobId();
        Job job = getJob(jobId);
        Suite suite = job.getSuite();
        //retrieve all jobs belong to this suite
        List<String> suiteJobs = getSuiteJobsIds(suite);
        Build build = job.getBuild();
        String branch = build.getBranch();
        Set<String> filterBranches = new HashSet<>();
        filterBranches.add(MASTER_BRANCH_NAME);
        filterBranches.add(branch);
        if( logger.isDebugEnabled() ) {
            logger.debug(
                "--getTests() history, testId=" + id + ",jobId=" + jobId + ", buildId=" + build
                    .getId() + ", branch=" + branch + ", endTime=" + endTime);
        }

        Query<Test> testsQuery = testDAO.createQuery();
        testsQuery.or(testsQuery.criteria("status").equal(Test.Status.FAIL), testsQuery.criteria("status").equal(Test.Status.SUCCESS)); // get only success or fail test
        testsQuery.order("-endTime"); // order by end time
        testsQuery.field("name").equal(testName).field("arguments").equal(testArguments);

        //in order to improve query in the case of master branch
        if (branch.equals(MASTER_BRANCH_NAME)) {
            String sha = thisTest.getSha();
            testsQuery.field("sha").equal(sha);
        }
        testsQuery.limit(limit);

        List<Test> tests = testDAO.find(testsQuery).asList();
        if( logger.isDebugEnabled() ) {
            logger.debug("--getTests() history, testId=" + id + ", tests size:" + tests.size());
        }
        //logger.info("DEBUG (getTests) get test history of testId: [{}], (thisTest: [{}])", id, thisTest);
        List<TestHistoryItem> testHistoryItemsList = new ArrayList<>(tests.size());
        for (Test test : tests) {
            if( logger.isDebugEnabled() ) {
                logger.debug(
                    "--getTests() history, test.getEndTime()=" + test.getEndTime() + ", tests size:"
                    + tests.size());
            }
            String jobIdLocal = test.getJobId();
            //don't bring tests that were ran after this test on any branch
            if (suiteJobs.contains(jobIdLocal) && (endTime == null || (test.getEndTime() != null && test.getEndTime().compareTo(endTime) <= 0))) {
                //logger.info("DEBUG (getTests) ---- > create testHistoryItem to [{}]", test);
                TestHistoryItem testHistoryItem = createTestHistoryItem(test, filterBranches);
                if (testHistoryItem == null) {
                    continue;
                }
                //logger.info("DEBUG (getTests) ---- > testHistoryItem is: [{}]", testHistoryItem);
                testHistoryItemsList.add(testHistoryItem);
            }
        }

        return new Batch<>(testHistoryItemsList, offset, limit, false, Collections.emptyList(), uriInfo);
    }

    //retrieve all jobs belong to this suite
    private List<String> getSuiteJobsIds(Suite suite) {
        String suiteId = suite.getId();
        Query<Job> query = jobDAO.createQuery();
        query.field("suite.id").equal(suiteId);
        List<ObjectId> jobObjectOIds = jobDAO.findIds(query);
        List<String> retVal = new ArrayList<>(jobObjectOIds.size());
        for (ObjectId jobObjectId : jobObjectOIds) {
            retVal.add(jobObjectId.toString());
        }

        return retVal;
    }


    private TestHistoryItem createTestHistoryItem(Test test, Collection<String> filterBranches) {

        Job job = getJob(test.getJobId());
        if (job == null) {
            return null;
        }
        String branch = job.getBuild().getBranch();
        if (!filterBranches.contains(branch)) {
            return null;
        }
        //logger.info("DEBUG (createTestHistoryItem) get job of test ---- > test: [{}], job: [{}])", test, job);
        return new TestHistoryItem(new TestView(test), new JobView(job));
    }

    private SuiteWithJobs createSuiteWithJobs(Suite suite) {

        String suiteId = suite.getId();
        Query<Job> jobsQuery = jobDAO.createQuery();
        jobsQuery.field("suite.id").equal(suiteId);
        jobsQuery.criteria("state").equal(State.DONE);
        jobsQuery.order("-endTime").limit(maxJobsPerSuite);

        addRequiredJobTableColumns(jobsQuery);

        List<Job> jobsList = jobDAO.find(jobsQuery).asList();
        return new SuiteWithJobs(suite, jobsList);
    }

    private Map<String, List<Job>> createActiveJobsMap(List<Build> builds) {
        Map<String, List<Job>> resultsMap = new HashMap<>();
        for (Build build : builds) {
            resultsMap.put(build.getId(), getActiveBuildJobs(build));
        }

        return resultsMap;
    }

    private List<Job> getActiveBuildJobs(Build build) {

        Query<Job> query = jobDAO.createQuery();
        query.retrievedFields(true, "id", "passedTests", "failedTests", "runningTests", "totalTests",
                "failed3TimesTests", "numOfTestRetries", "suite.id", "suite.name");
        query.field("build.id").equal(build.getId()).field("state").equal(State.RUNNING);
        return jobDAO.find(query).asList();
    }

    @POST
    @Path("clearPaused")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearPausedJobs() throws InterruptedException {
        Query<Job> query = jobDAO.createQuery();
        query.criteria("state").equal(State.PAUSED);
        try {
            for (Job j : jobDAO.find(query).asList()) {
                deleteJob(j.getId());
            }
        } catch (Exception e) {
            logger.warn("caught exception during clear paused jobs operation", e);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("pauseJobsAndWait")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response suspend() throws InterruptedException {
        UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations();
        jobUpdateOps.set("state", State.PAUSED);

        try {
            Query<Job> updateStateQuery = jobDAO.createQuery();
            updateStateQuery.or(updateStateQuery.criteria("state").equal(State.RUNNING), updateStateQuery.criteria("state").equal(State.READY));
            jobDAO.getDatastore().update(updateStateQuery, jobUpdateOps);

            final Query<Job> jobQuery = jobDAO.createQuery();
            jobQuery.criteria("state").equal(State.PAUSED);
            final Query<Job> stillRunningJobsQuery = jobQuery.filter("runningTests >", 0);

            QueryResults<Job> runningJobsResult = jobDAO.find(stillRunningJobsQuery);

            while (!runningJobsResult.asList().isEmpty()) {
                List<Job> jobList = runningJobsResult.asList();
                logger.info("waiting for all agents to finish running tests, {} jobs are still running:", jobList.size());
                for (Job job : jobList) {
                    logger.info("{},", job.getId());
                }
                Thread.sleep(10000);
                runningJobsResult = jobDAO.find(stillRunningJobsQuery);
            }
        } catch (Exception e) {
            logger.warn("failed to suspend server", e);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }


    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServerStatus() {
        synchronized (this.serverStatusLock) {
            return Response.ok().entity(this.serverStatus).build();
        }
    }

    @POST
    @Path("unsuspend")
    @Produces(MediaType.TEXT_PLAIN)
    public Response unsuspendServer() {
        synchronized (this.serverStatusLock) {
            if (this.serverStatus.getStatus().equals(ServerStatus.Status.RUNNING)) {
                return Response.status(Response.Status.FORBIDDEN).entity("Server is already running").build();
            }
            serverSuspendThread.interrupt();
            this.serverStatus.setStatus(ServerStatus.Status.RUNNING);
            broadcastMessage(MODIFY_SERVER_STATUS, serverStatus);
        }
        return Response.ok().build();
    }

    @POST
    @Path("suspend")
    @Produces(MediaType.APPLICATION_JSON)
    public Response suspendServer() {
        synchronized (this.serverStatusLock) {
            if (this.serverStatus.getStatus().equals(ServerStatus.Status.SUSPENDING)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Server is currently suspending").build();
            } else if (this.serverStatus.getStatus().equals(ServerStatus.Status.SUSPENDED)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Server is already suspended").build();
            }
            this.serverStatus.setStatus(ServerStatus.Status.SUSPENDING);
            broadcastMessage(MODIFY_SERVER_STATUS, serverStatus);
        }

        serverSuspendThread = new Thread(() -> {
            try {
                while (true) {
                    List<FutureJob> futureJobs = futureJobDAO.createQuery().asList();
                    if (futureJobs.size() == 0) {
                        synchronized (this.serverStatusLock) {
                            serverStatus.setStatus(ServerStatus.Status.SUSPENDED);
                            broadcastMessage(MODIFY_SERVER_STATUS, serverStatus);
                            break;
                        }
                    } else {
                        Thread.sleep(10000);
                    }
                }
            } catch (InterruptedException | MongoInterruptedException e) {
            } catch (Exception e) {
                logger.warn("failed to suspend server", e);
                synchronized (this.serverStatusLock) {
                    serverStatus.setStatus(ServerStatus.Status.SUSPEND_FAILED);
                    broadcastMessage(MODIFY_SERVER_STATUS, serverStatus);
                }
            }
        });
        serverSuspendThread.start();

        return Response.ok().build();
    }

    private java.nio.file.Path saveFile(InputStream is, String location) throws IOException {
        java.nio.file.Path path = Paths.get(location);
        Files.createDirectories(path.getParent());
        try {
            Files.copy(is, Paths.get(location), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            is.close();
        }
        return path;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("agent/{setupRetries}")
    public Agent setNumberOfRetries(@PathParam("setupRetries") final int numberOfRetries, final Agent agent) {

        int setupRetriesBefore = agent.getSetupRetries();
        UpdateOperations<Agent> agentUpdateOps = agentDAO.createUpdateOperations();
        agentUpdateOps.set("setupRetries", numberOfRetries);
        Agent updatedAgent = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()), agentUpdateOps, false, false);
        if (updatedAgent != null) {
            broadcastMessage(MODIFIED_AGENT, updatedAgent);
            if ((setupRetriesBefore==0 && numberOfRetries>0) || (setupRetriesBefore>0 && numberOfRetries==0)) {
                long count = agentDAO.createQuery().filter("setupRetries >", 0).countAll();
                broadcastMessage(MODIFIED_FAILING_AGENTS, count);
            }
        }
        return updatedAgent;
    }


    // events

    private void broadcastMessage(String type, Object value) {
        if (value != null) {
            try {
                long time1 = System.currentTimeMillis();
                EventSocket.broadcast(new Message(type, value));
                long time2 = System.currentTimeMillis();
                if( logger.isDebugEnabled() ) {
                    logger.debug(
                        "Broadcasting message [" + type + "] with value [" + value + "] took " + (
                            time2 - time1) + " ms");
                }
            } catch (Throwable ignored) {
                logger.error("Invoking of broadcastMessage() failed");
            }
        }
    }

    private List<Agent> getAgentsNotSeenInLastMillis(long delay) {
        return agentDAO.find(agentDAO.createQuery().field("state").notEqual(Agent.State.IDLING).field("lastTouchTime").lessThan(new Date(System.currentTimeMillis() - delay))).asList();
    }

    private List<Agent> getZombieAgents(long delay) {
        return agentDAO.find(agentDAO.createQuery().field("state").equal(Agent.State.IDLING).field("lastTouchTime").lessThan(new Date(System.currentTimeMillis() - delay))).asList();
    }

    private void handleZombieAgent(Agent agent) {
        logger.warn("Agent {} did not report on time while he was IDLING and will be deleted", agent.getName());
        final Agent toDelete = agentDAO.findOne(agentDAO.createQuery().field("name").equal(agent.getName()));
        if (toDelete != null) {
            String agentIp = agent.getHostAddress();
            offlineAgents.put(agentIp, new OfflineAgent(agentIp, agent.getHost(), agent.getHostAddress(), agent.getLastTouchTime()));
            agentDAO.getDatastore().findAndDelete(agentDAO.createIdQuery(toDelete.getId()));
            broadcastMessage(DELETED_AGENT, toDelete.getId());
            broadcastMessage(CREATED_OFFLINE_AGENT, createAgentFromOfflineAgent(offlineAgents.get(agentIp)));
            broadcastMessage(MODIFIED_AGENTS_COUNT, agentDAO.count());
            //Delete agent from preparing agents in jobs
            jobDAO.createUpdateOperations().removeAll("preparingAgents", toDelete.getName());
        }
    }

    private void handleUnseenAgent(Agent agent) {
        logger.warn("Agent {} is did not report on time", agent.getName());
        returnTests(agent);

    }

    /**
     * Return this agent job and test to the ool, update agent data.
     *
     * @param agent the agent in hand.
     */
    private void returnTests(Agent agent) {
        Set<Test> tests = new HashSet<>();
        for (String testId : agent.getCurrentTests()) {
            Test found = testDAO.getDatastore().findAndModify(testDAO.createIdQuery(testId).field("status").equal(Test.Status.RUNNING)
                            .field("assignedAgent").equal(agent.getName()),
                    testDAO.createUpdateOperations().unset("assignedAgent").unset("startTime").set("status", Test.Status.PENDING));
            if (found != null) {
                logger.warn("test {} was released since agent {} not seen for a long time", found.getId(), agent.getName());
                tests.add(found);
            }
        }
        Job job = null;
        if (agent.getJobId() != null) {
            UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations();
            if (agent.getState() == Agent.State.PREPARING) {
                jobUpdateOps.removeAll("preparingAgents", agent.getName());
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(agent.getJobId()), jobUpdateOps);
            } else if (agent.getState() == Agent.State.RUNNING && !tests.isEmpty()) {
                int runningTests = 0 - tests.size();
                logger.info("returnTests for agent [{}], jobId [{}], running tests size [{}]",
                        agent.getName(), agent.getJobId(), runningTests);
                jobUpdateOps.inc("runningTests", runningTests);
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(agent.getJobId()), jobUpdateOps);
            }
        }
        Agent ag = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()),
                agentDAO.createUpdateOperations().set("currentTests", new HashSet<>()).set("state", Agent.State.IDLING));
        ag.setJob(job);

        if (ag != null && ag.getState().equals(Agent.State.IDLING)) {
            logger.warn("agent [{}] is idling while returning tests to pool", ag.getName());
        }
        for (Test test : tests) {
            broadcastMessage(MODIFIED_TEST, test);
        }
        if (job != null) {
            broadcastMessage(MODIFIED_JOB, job);
        }
        if (ag != null) {
            broadcastMessage(MODIFIED_AGENT, ag);
        }
    }

    private void checkServerStatus() {
        synchronized (this.serverStatusLock) {
            if (this.serverStatus.getStatus().equals(ServerStatus.Status.SUSPENDED)) {
                throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE).entity("Server is suspended, please try again later.").build());
            }
        }
    }

}
