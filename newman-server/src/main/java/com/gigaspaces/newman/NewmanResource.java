package com.gigaspaces.newman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.config.Config;
import com.gigaspaces.newman.dao.*;
import com.gigaspaces.newman.utils.FileUtils;
import com.mongodb.MongoClient;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ChunkedOutput;
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final String CREATED_BUILD = "created-build";
    public static final String CREATED_SUITE = "created-suite";
    public static final String MODIFIED_SUITE = "modified-suite";
    public static final String CREATE_FUTURE_JOB = "create-future-job";

    private final SseBroadcaster broadcaster;
    private final MongoClient mongoClient;
    private final JobDAO jobDAO;
    private final TestDAO testDAO;
    private final BuildDAO buildDAO;
    private final AgentDAO agentDAO;
    private final SuiteDAO suiteDAO;
    private final FutureJobDAO futureJobDAO;
    private final Config config;
    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "tests-logs";
    @SuppressWarnings("FieldCanBeLocal")
    private final Timer timer = new Timer(true);

    private final ConcurrentHashMap<String, Object> agentLocks = new ConcurrentHashMap<>();

    private static final int maxJobsPerSuite = 5;
    private final DistinctIterable distinctTestsByAssignedAgentFilter;

    private final static String CRITERIA_PROP_NAME = "criteria";

    public NewmanResource(@Context ServletContext servletContext) {
        this.config = Config.fromString(servletContext.getInitParameter("config"));
        //noinspection SpellCheckingInspection
        this.broadcaster = new SseBroadcaster() {
            @Override
            public void onException(ChunkedOutput<OutboundEvent> chunkedOutput, Exception exception) {
                logger.error(exception.toString(), exception);
                remove(chunkedOutput);
            }

            @Override
            public void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
                remove(chunkedOutput);
                logger.info("onClose {}", chunkedOutput);
            }
        };
        mongoClient = new MongoClient(config.getMongo().getHost());
        Morphia morphia = new Morphia().mapPackage("com.gigaspaces.newman.beans.criteria").mapPackage("com.gigaspaces.newman.beans");
        Datastore ds = morphia.createDatastore(mongoClient, config.getMongo().getDb());
        ds.ensureIndexes();
        ds.ensureCaps();
        jobDAO = new JobDAO(morphia, mongoClient, config.getMongo().getDb());
        testDAO = new TestDAO(morphia, mongoClient, config.getMongo().getDb());
        buildDAO = new BuildDAO(morphia, mongoClient, config.getMongo().getDb());
        agentDAO = new AgentDAO(morphia, mongoClient, config.getMongo().getDb());
        suiteDAO = new SuiteDAO(morphia, mongoClient, config.getMongo().getDb());
        futureJobDAO = new FutureJobDAO(morphia, mongoClient, config.getMongo().getDb());

        MongoDatabase db = mongoClient.getDatabase(config.getMongo().getDb());
        MongoCollection testCollection = db.getCollection("Test");
        distinctTestsByAssignedAgentFilter = testCollection.distinct("assignedAgent", String.class);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getAgentsNotSeenInLastMillis(1000 * 60 * 3).forEach(NewmanResource.this::handleUnseenAgent);
            }
        }, 1000 * 30, 1000 * 30);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getZombieAgents(1000 * 60 * 20).forEach(NewmanResource.this::handleZombieAgent);
            }
        }, 1000 * 30, 1000 * 30);

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
                UpdateOperations<Test> ops = testDAO.createUpdateOperations().set("sha", Sha.compute(test.getName(), test.getArguments()));
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

        List<Job> jobs = retrieveJobs( buildId, orderBy, all, offset, limit );
        List<JobView> jobViews = new ArrayList<>( jobs.size() );

        for( Job job : jobs ){
            jobViews.add( new JobView( job ) );
        }
        return new Batch<>(jobViews, offset, limit, all, orderBy, uriInfo);
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

        List<Job> jobs = retrieveJobs( buildId, orderBy, all, offset, limit );
        return new Batch<>(jobs, offset, limit, all, orderBy, uriInfo);
    }

    private List<Job> retrieveJobs( String buildId, List<String> orderBy, boolean all, int offset, int limit ) {

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

        return jobDAO.find(query).asList();
    }


    @GET
    @Path("futureJob")
    @Produces(MediaType.APPLICATION_JSON)
    public FutureJob getAndDeleteFutureJob(
            @Context UriInfo uriInfo) {

        Query<FutureJob> query = futureJobDAO.createQuery();
        query.order("submitTime");
        FutureJob futureJob = futureJobDAO.findOne(query);
        if(futureJob != null){
            Datastore datastore = futureJobDAO.getDatastore();
            datastore.findAndDelete(query);
        }
        return futureJob;
    }

    @GET
    @Path("job/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job getJob(@PathParam("id") final String id) {

        Job job = jobDAO.findOne(jobDAO.createQuery().field("_id").equal(new ObjectId(id)));
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


    @DELETE
    @Path("jobs/{nubmerOfDaysToNotDelete}")
    @Produces(MediaType.APPLICATION_JSON)
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
                logger.debug("deleted job: " + job.getId() + " with build time " + job.getBuild().getBuildTime());
                jobsDeleted++;
            }
        }
        return "Deleted: " + jobsDeleted + " jobs from the last " + numberOfDays + " days.";
    }


    @PUT
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job createJob(JobRequest jobRequest, @Context SecurityContext sc) {
        Build build = buildDAO.findOne(buildDAO.createIdQuery(jobRequest.getBuildId()));
        Suite suite = null;
        if (jobRequest.getSuiteId() != null) {
            suite = suiteDAO.findOne(suiteDAO.createIdQuery(jobRequest.getSuiteId()));
        }
        if (suite == null) {
            throw new BadRequestException("invalid suite id for Job request: " + jobRequest);
        }

        if (build != null) {
            Job job = new Job();
            job.setBuild(build);
            job.setSuite(suite);
            job.setState(State.READY);
            job.setSubmitTime(new Date());
            job.setSubmittedBy(sc.getUserPrincipal().getName());
            jobDAO.save(job);
            UpdateOperations<Build> buildUpdateOperations = buildDAO.createUpdateOperations().inc("buildStatus.totalJobs")
                    .inc("buildStatus.pendingJobs")
                    .add("buildStatus.suitesIds", suite.getId(), false)
                    .add("buildStatus.suitesNames", suite.getName(), false);
            build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(build.getId()), buildUpdateOperations);
            broadcastMessage(CREATED_JOB, job);
            broadcastMessage(MODIFIED_BUILD, build);
            broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(suite));
            return job;
        } else {
            return null;
        }
    }

    @POST
    @Path("futureJob/{buildId}/{suiteId}")
    @Produces(MediaType.APPLICATION_JSON)
    public FutureJob createFutureJob(
            @PathParam("buildId") String buildId,
            @PathParam("suiteId") String suiteId,
            @Context SecurityContext sc){
        String author = sc.getUserPrincipal().getName();
        Build build = null;
        Suite suite = null;

        if(buildId != null){
            build =  buildDAO.findOne(buildDAO.createIdQuery(buildId));
            if(build == null) throw new BadRequestException("invalid build id in create FutureJob: " + buildId);
        }
        if(suiteId != null){
            suite = suiteDAO.findOne(suiteDAO.createIdQuery(suiteId));
            if(suite == null) throw new BadRequestException("invalid suite id in create FutureJob: " + suiteId);
        }

        FutureJob futureJob = new FutureJob();
        if (build != null) {
            futureJob.setBuildID(build.getId());
        }
        if (suite != null) {
            futureJob.setSuiteID(suite.getId());
        }
        futureJob.setSubmitTime(new Date());
        futureJob.setAuthor(author);

        futureJobDAO.save(futureJob);
        broadcastMessage(CREATE_FUTURE_JOB,futureJob);
        return futureJob;
    }

    @POST
    @Path("job/{id}/toggle")
    @Produces(MediaType.APPLICATION_JSON)
    public Job toggelJobPause(@PathParam("id") final String id) {
        Job job = jobDAO.findOne(jobDAO.createQuery().field("_id").equal(new ObjectId(id)));
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
//                broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs( job.getSuite() ) );
                return job;
            }
        }
        return null;
    }

    @GET
    @Path("dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardData getActiveJobGroup(@Context UriInfo uriInfo) {
        List<Build> activeBuilds =
                buildDAO.find(buildDAO.createQuery().
                        where("this.buildStatus.runningJobs>0").
                        where("this.buildStatus.totalJobs>0").
                        where("this.buildStatus.doneJobs < this.buildStatus.totalJobs").
                        order("-buildTime")).
                        asList();
        List<Build> pendingBuilds =
                buildDAO.find(buildDAO.createQuery().
                        where("this.buildStatus.pendingJobs>0").
                        where("this.buildStatus.runningJobs==0").
                        where("this.buildStatus.totalJobs>0").
                        where("this.buildStatus.doneJobs < this.buildStatus.totalJobs").
                        limit(5).
                        order("-buildTime")).
                        asList();
        List<Build> historyBuilds =
                buildDAO.find(buildDAO.createQuery().
                        where("this.buildStatus.totalJobs>0").
                        where("this.buildStatus.doneJobs == this.buildStatus.totalJobs").
                        limit(5).
                        order("-buildTime")).
                        asList();

        Map<String, List<Job>> activeJobsMap = createActiveJobsMap(activeBuilds);

        return new DashboardData(activeBuilds, pendingBuilds, historyBuilds, activeJobsMap);
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
    public void addTests(Batch<Test> tests) {
        if (tests.getValues().isEmpty()) {
            return;
        }
        Job job = jobDAO.findOne(jobDAO.createIdQuery(tests.getValues().get(0).getJobId()));
        if (job == null) {
            return;
        }
        List<Test> res = new ArrayList<>(tests.getValues().size());
        //noinspection Convert2streamapi
        for (Test test : tests.getValues()) {
            res.add(addTest(test));
        }
        //res.addAll(tests.getValues().stream().map(this::addTest).collect(Collectors.toList()));
        if (!res.isEmpty()) {
            Test test = res.get(0);
            UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations().inc("totalTests", res.size());
            job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(test.getJobId()), jobUpdateOps, false, false);
            Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()), buildDAO.createUpdateOperations().inc("buildStatus.totalTests", res.size()));
            tests.getValues().stream().forEach(test1 -> broadcastMessage(CREATED_TEST, test1));
            broadcastMessage(MODIFIED_BUILD, build);
            broadcastMessage(MODIFIED_JOB, job);
        }
//        return new Batch<>(res, tests.getOffset(), tests.getLimit(), false, Collections.emptyList(), null);
    }


    // return all builds that need to be submit, e.g there are NO jobs that run with them
    @GET
    @Path("build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Build> getPendingBuildsToSubmit(
            @QueryParam("branches") String branchesStr,
            @QueryParam("tags") String tagsStr) {

        Set<String> tagsSet = null;
        if (branchesStr == null || branchesStr.isEmpty()){
            throw new IllegalArgumentException("branches can not be null or empty when asking for pending builds to submit");
        }
        List<String> branches = Arrays.asList(branchesStr.split("\\s*,\\s*"));
        if(tagsStr != null && !tagsStr.isEmpty()){
            tagsSet = new HashSet<>(Arrays.asList(tagsStr.split("\\s*,\\s*")));
        }
        List<Build> buildsRes = new ArrayList<>();

        for (String branch : branches) {
            Query<Build> query = buildDAO.createQuery().order("-buildTime").field("branch").equal(branch);

            if(tagsSet != null && !tagsSet.isEmpty()){ // build should be with specifics tags
                query.field("tags").hasAllOf(tagsSet);
            }

            Build build = buildDAO.findOne(query);
            if (build != null && build.getBuildStatus().getTotalJobs() == 0) {
                buildsRes.add(build);
            }
        }
        return new Batch<>(buildsRes, 0, buildsRes.size(), true, null, null);
    }

    private Test addTest(Test test) {
        if (test.getJobId() == null) {
            throw new BadRequestException("can't add test with no jobId: " + test);
        }
        test.setStatus(Test.Status.PENDING);
        test.setScheduledAt(new Date());
        test.setSha(Sha.compute(test.getName(), test.getArguments()));
        testDAO.save(test);
        return test;
    }


    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public synchronized Test finishTest(final Test test) {
        try{
            logger.info("trying to finish test {}.", test);
            if (test.getId() == null) {
                throw new BadRequestException("can't finish test without testId: " + test);
            }
            if(getJob(test.getJobId()) == null){
                throw new BadRequestException("finishTest - the job of the test is not on database. test: ["+test+"].");
            }
            Test.Status status = test.getStatus();
            if (status == null || (status != Test.Status.FAIL && status != Test.Status.SUCCESS)) {
                throw new BadRequestException("can't finish test without state set to success or fail state" + test);
            }
            UpdateOperations<Test> testUpdateOps = testDAO.createUpdateOperations();
            UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations();
            UpdateOperations<Build> updateBuild = buildDAO.createUpdateOperations();
            testUpdateOps.set("status", status);
            if (status == Test.Status.FAIL) {
                updateJobStatus.inc("failedTests");
                updateBuild.inc("buildStatus.failedTests");
            } else {
                updateJobStatus.inc("passedTests");
                updateBuild.inc("buildStatus.passedTests");
            }
            updateJobStatus.dec("runningTests");
            updateBuild.dec("buildStatus.runningTests");
            logger.info("DEBUG(finishTest) ---> prepare job update because test: id:[{}], name:[{}], jobId:[{}]", test.getId(), test.getName(), test.getJobId());

            if (test.getErrorMessage() != null) {
                testUpdateOps.set("errorMessage", test.getErrorMessage());
            }
            if (status == Test.Status.FAIL || status == Test.Status.SUCCESS) {
                testUpdateOps.set("endTime", new Date());
            }
            int historyLength = 25;
            List<TestHistoryItem> testHistory = getTests(test.getId(), 0, historyLength, null).getValues();
            String historyStatsString = TestScoreUtils.decodeShortHistoryString(testHistory, test.getStatus()); // added current fail to history;
            double reliabilityTestScore = TestScoreUtils.score(historyStatsString);

            testUpdateOps.set("testScore", reliabilityTestScore);
            testUpdateOps.set("historyStats", historyStatsString);
            logger.info("got test history [{}] of test and prepare to update:  id:[{}], name:[{}], jobId:[{}]", historyStatsString, test.getId(), test.getName(), test.getJobId());

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
            logger.info("DEBUG(finishTest) ---> modify job: [{}], testResult: [{}]", job, result);
            Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()), updateBuild);

            if (result.getAssignedAgent() != null) {
                UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date())
                        .removeAll("currentTests", test.getId());
                Agent agent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(result.getAssignedAgent()), updateOps, false, false);
                if (agent != null) {
                    logger.info("DEBUG(finishTest) find and update agent ---> agent: [{}]", agent.getName());
                    Agent idling = null;
                    if (agent.getCurrentTests().isEmpty()) {
                        idling = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(result.getAssignedAgent())
                                        .where("this.currentTests.length == 0"),
                                agentDAO.createUpdateOperations().set("state", Agent.State.IDLING));
                    }
                    broadcastMessage(MODIFIED_AGENT, idling == null ? agent : idling);
                }
            }
            broadcastMessage(MODIFIED_BUILD, build);
            broadcastMessage(MODIFIED_TEST, result);
            broadcastMessage(MODIFIED_JOB, job);
            broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(job.getSuite()));
            logger.info("succeed finish test {}.", result);
            return result;
        }
        catch (Exception e){
            logger.error("failed to finish test because: ", e);
            e.printStackTrace();
            throw e;
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
        List<TestView> testsView = new ArrayList<>( tests.size() );
        for( Test test : tests ){
            testsView.add( new TestView( test ) );
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

        List<Test> tests = retrieveJobTests(jobId, orderBy, all, offset, limit);
        return new Batch<>(tests, offset, limit, all, orderBy, uriInfo);
    }

    private List<Test> retrieveJobTests( String jobId, List<String> orderBy, boolean all, int offset, int limit ){

        Query<Test> query = testDAO.createQuery();
        if (jobId != null) {
            query.field("jobId").equal(jobId);
        }
        if (orderBy != null) {
            if (orderBy.isEmpty()) {
                orderBy.add("testScore");
            }
            orderBy.forEach(query::order);
        }
        if (!all) {
            query.offset(offset).limit(limit);
        }

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
    public Test uploadLog(FormDataMultiPart form,
                          @PathParam("jobId") String jobId,
                          @PathParam("id") String id,
                          @Context UriInfo uriInfo) {
        if(getJob(jobId) == null){
            logger.warn("uploadLog - the job of the test is not on database. testId:[{}], jobId:[{}].", id, jobId);
            return null;
        }
        FormDataBodyPart filePart = form.getField("file");
        ContentDisposition contentDispositionHeader = filePart.getContentDisposition();
        InputStream fileInputStream = filePart.getValueAs(InputStream.class);
        String fileName = contentDispositionHeader.getFileName();

        if (fileName.toLowerCase().endsWith(".zip")) {
            handleLogBundle(id, jobId, uriInfo, fileInputStream, fileName);
        } else {
            handleLogFile(id, jobId, uriInfo, fileInputStream, fileName);
        }

        return null;
    }

    private void handleLogFile(String testId, String jobId, UriInfo uriInfo, InputStream fileInputStream, String fileName) {
        String filePath = calculateTestLogFilePath(jobId, testId) + fileName;
        try {
            saveFile(fileInputStream, filePath);
            URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();
            String name = getLogName(fileName);
            UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("logs." + name, uri.toASCIIString());
            Test test = testDAO.getDatastore().findAndModify(testDAO.createIdQuery(testId), updateOps);
            broadcastMessage(MODIFIED_TEST, test);
        } catch (IOException e) {
            logger.error("Failed to save log at {} for test {} jobId {}", filePath, testId, jobId, e);
        }
    }

    private static String calculateTestLogFilePath(String jobId, String testId) {
        return SERVER_UPLOAD_LOCATION_FOLDER + "/" + jobId + "/" + testId + "/";
    }

    private void handleLogBundle(String testId, String jobId, UriInfo uriInfo, InputStream fileInputStream, String fileName) {

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
    public Response downloadLog(@PathParam("jobId") String jobId, @PathParam("id") String id, @PathParam("name") String name,
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
    @Path("log/size")
    public Response computeLogDirSize() {
        try {
            if (!new File(SERVER_UPLOAD_LOCATION_FOLDER).exists()) {
                return Response.ok("0").build();
            }
            return Response.ok(String.valueOf(Files.walk(Paths.get(SERVER_UPLOAD_LOCATION_FOLDER)).mapToLong(p -> p.toFile().length()).sum()), MediaType.TEXT_PLAIN_TYPE).build();
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }
    }


    @DELETE
    @Path("log")
    @RolesAllowed("admin")
    public Response deleteLogs() {
        try {
            java.nio.file.Path path = Paths.get(SERVER_UPLOAD_LOCATION_FOLDER);
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
    @Path("ping/{name}/{jobId}/{testId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String ping(@PathParam("name") final String name, @PathParam("jobId") final String jobId
            , @PathParam("testId") final String testId) {
        UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date());
        Agent agent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(name), updateOps, false, false);
        if (agent == null) {
            logger.error("Unknown agent " + name);
            return null;
        }
        if (agent.getJobId() == null) {
            logger.error("Agent {} not working on job {} test {}", agent, jobId, testId);
            return null;
        } else {
            broadcastMessage(MODIFIED_AGENT, agent);
            return agent.getJobId();
        }
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
    public Agent getAgent(@PathParam("name") final String name) {
        return agentDAO.findOne(agentDAO.createQuery().field("name").equal(name));
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
        return new Batch<>(agentDAO.find(query).asList(), offset, limit, all, orderBy, uriInfo);
    }

    @POST
    @Path("agent/{name}/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("name") final String name, @PathParam("jobId") final String jobId) {
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
        Query<Test> query = testDAO.createQuery();
        query.and(query.criteria("jobId").equal(jobId), query.criteria("status").equal(Test.Status.PENDING));
        UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("status", Test.Status.RUNNING)
                .set("assignedAgent", name).set("startTime", new Date());
        Test result = testDAO.getDatastore().findAndModify(query, updateOps, false, false);

        if (result != null) {
            UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().inc("runningTests");
            Job job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId).field("state").notEqual(State.PAUSED), updateJobStatus);
            if (job != null) {
                UpdateOperations<Build> buildUpdateOperations = buildDAO.createUpdateOperations().inc("buildStatus.runningTests");
                if (job.getStartTime() == null) {
                    updateJobStatus = jobDAO.createUpdateOperations().set("startTime", new Date()).set("state", State.RUNNING);
                    job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId).field("state").notEqual(State.PAUSED), updateJobStatus);
                    logger.info("DEBUG (getTest) ---> update job: [{}]", job);
                    logger.info("DEBUG (getTest) ---> Test cause job update: [{}].", result);
                    buildUpdateOperations.inc("buildStatus.runningJobs").dec("buildStatus.pendingJobs");
                }
                agentUpdateOps.add("currentTests", result.getId());
                agentUpdateOps.set("state", Agent.State.RUNNING);
                Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()),
                        buildUpdateOperations);
                broadcastMessage(MODIFIED_TEST, result);
                broadcastMessage(MODIFIED_JOB, job);
//                broadcastMessage(MODIFIED_SUITE,createSuiteWithJobs( job.getSuite() ) );
                broadcastMessage(MODIFIED_BUILD, build);
            } else {
                // return the test to the pool.
                updateOps = testDAO.createUpdateOperations().set("status", Test.Status.PENDING)
                        .unset("assignedAgent").unset("startTime");
                testDAO.getDatastore().findAndModify(query, updateOps, false, false);

                agent = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()),
                        agentDAO.createUpdateOperations().removeAll("currentTests", result.getId()), false, true);
                if (agent.getCurrentTests().isEmpty()) {
                    Agent idling = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()).where("this.currentTests.length == 0"),
                            agentDAO.createUpdateOperations().set("state", Agent.State.IDLING));
                    if (idling != null) {
                        agent = idling;
                    }
                }

                broadcastMessage(MODIFIED_AGENT, agent);
                if (pj != null) {
                    broadcastMessage(MODIFIED_JOB, pj);
//                    broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs( pj.getSuite() ) );
                }
                return null;
            }
        }
        agent = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()), agentUpdateOps, false, true);
        broadcastMessage(MODIFIED_AGENT, agent);
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
    public Job subscribe(final Agent agent) {
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
                    broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(oldJob.getSuite()));
                }
            } else if (found.getState() == Agent.State.RUNNING && !found.getCurrentTests().isEmpty() && found.getJobId() != null) {
                returnTests(found);
            }
        }

        Query<Job> basicQuery = jobDAO.createQuery();
        basicQuery.or(basicQuery.criteria("state").equal(State.READY), basicQuery.criteria("state").equal(State.RUNNING));
        basicQuery.where("this.totalTests != (this.passedTests + this.failedTests + this.runningTests)");
        basicQuery.order("submitTime");

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
            job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(job.getId()), updateJobStatus);
            broadcastMessage(MODIFIED_JOB, job);
            broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(job.getSuite()));
        } else {
            updateOps.set("state", Agent.State.IDLING);
        }

        Agent readyAgent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(agent.getName()), updateOps, false, true);


        broadcastMessage(MODIFIED_AGENT, readyAgent);
        return job;
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
            job = bestMatch(jobsFilterByCapabilities);
        }
        if (job == null) { // search for jobs without requirements
            Query<Job> noRequirementsQuery = basicQuery.cloneQuery();
            noRequirementsQuery.field("suite.requirements").doesNotExist();
            job = jobDAO.findOne(noRequirementsQuery);
        }
        return job;
    }

    private Job bestMatch(List<Job> jobsFilterByCapabilities) {
        Job job = null;
        List<Job> jobsGroupById = groupByBuild(jobsFilterByCapabilities);
        if (jobsGroupById != null && !jobsGroupById.isEmpty()) { // if has jobs after filter
            Collections.sort(jobsGroupById, CapabilitiesAndRequirements.requirementsSort);
            job = jobsGroupById.get(0);
        }
        return job;
    }

    private List<Job> groupByBuild(List<Job> jobs) {
        if (jobs.isEmpty()) return null;
        String BuildId = jobs.get(0).getBuild().getId();
        return jobs.stream().filter(job -> BuildId.equals(job.getBuild().getId())).collect(Collectors.toList());
    }

    public Build getLatestBuild() {
        return buildDAO.findOne(buildDAO.createQuery().order("-buildTime"));
    }

    @GET
    @Path("build/latest/{tags}")
    @Produces(MediaType.APPLICATION_JSON)
    public Build getLatestBuildWithTags(final @PathParam("tags") String tags) {
        if (tags == null || tags.isEmpty()) {
            return getLatestBuild();
        }
        Set<String> tagsSet = new HashSet<>(Arrays.asList(tags.split("\\s*,\\s*")));
        return buildDAO.findOne(buildDAO.createQuery().field("tags").hasAllOf(tagsSet).order("-buildTime"));
    }

    @GET
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<Build> getBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("30") @QueryParam("limit") int limit,
                                  @DefaultValue("false") @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {Query<Build> query = buildDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
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
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build updateBuild(final @PathParam("id") String id, final Build build) {
        logger.info("---updateBuild()");
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
        Query<Build> query = buildDAO.createIdQuery(id);
        Build result = buildDAO.getDatastore().findAndModify(query, updateOps);
        if (result != null) {
            broadcastMessage(MODIFIED_BUILD, result);
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
        logger.info("build=" + build);
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
        updateBuildWithDeletedJob(deletedJob);
        performDeleteTests(jobId);
        return Response.ok(Entity.json(jobId)).build();
    }

    private void performDeleteTestsLogs(String jobId) {
        java.nio.file.Path path = Paths.get(SERVER_UPLOAD_LOCATION_FOLDER + "/" + jobId);
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
        return Response.ok(Entity.json(agentId)).build();
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
        Query<Suite> query = suiteDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }

        List<Suite> suites = suiteDAO.find(query).asList();
        List<SuiteView> suiteViews = new ArrayList<>( suites.size() );
        for( Suite suite : suites ){
            suiteViews.add( new SuiteView( suite ) );
        }

        return new Batch<>(suiteViews, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("suites-dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSuitesWithJobs() {
        Query<Suite> suitesQuery = suiteDAO.createQuery();

        List<Suite> suites = suiteDAO.find(suitesQuery).asList();
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
        final String sha = thisTest.getSha();

        Query<Test> testsQuery = testDAO.createQuery();
        testsQuery.or(testsQuery.criteria("status").equal(Test.Status.FAIL), testsQuery.criteria("status").equal(Test.Status.SUCCESS)); // get only success or fail test
        testsQuery.order("-endTime"); // order by end time
        testsQuery.field("sha").equal(sha);
        testsQuery.limit(limit);

        List<Test> tests = testDAO.find(testsQuery).asList();
        logger.info("DEBUG (getTests) get test history of testId: [{}], (thisTest: [{}])", id, thisTest);
        List<TestHistoryItem> testHistoryItemsList = new ArrayList<>(tests.size());
        for (Test test : tests) {
            logger.info("DEBUG (getTests) ---- > create testHistoryItem to [{}]", test);
            TestHistoryItem testHistoryItem = createTestHistoryItem(test);
            if (testHistoryItem == null) {
                continue;
            }
            logger.info("DEBUG (getTests) ---- > testHistoryItem is: [{}]", testHistoryItem);
            testHistoryItemsList.add(testHistoryItem);
        }
        return new Batch<>(testHistoryItemsList, offset, limit, false, Collections.emptyList(), uriInfo);
    }


    private TestHistoryItem createTestHistoryItem(Test test) {

        Job job = getJob(test.getJobId());
        if (job == null) {
            return null;
        }
        logger.info("DEBUG (createTestHistoryItem) get job of test ---- > test: [{}], job: [{}])", test, job);
        return new TestHistoryItem( new TestView( test ), new JobView( job ) );
    }

    private SuiteWithJobs createSuiteWithJobs(Suite suite) {

        String suiteId = suite.getId();
        Query<Job> jobsQuery = jobDAO.createQuery();
        jobsQuery.field("suite.id").equal(suiteId);
        jobsQuery.criteria("state").equal(State.DONE);
        jobsQuery.order("-endTime").limit(maxJobsPerSuite);

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
        query.field("build.id").equal(build.getId()).field("state").equal(State.RUNNING);
        return jobDAO.find(query).asList();
    }

    @GET
    @Path("event")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput listenToBroadcast() {
        final EventOutput eventOutput = new EventOutput();
        this.broadcaster.add(eventOutput);
        return eventOutput;
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
        }
        catch (Exception e){
            logger.warn("caught exception during clear paused jobs operation", e);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("suspend")
    @Produces(MediaType.APPLICATION_JSON)
    public Response suspend() throws InterruptedException {
            UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations();
        jobUpdateOps.set("state", State.PAUSED);
        try {
            Query<Job> query = jobDAO.createQuery();
            query.or(query.criteria("state").equal(State.RUNNING), query.criteria("state").equal(State.READY));
            jobDAO.getDatastore().update(query, jobUpdateOps);
            final Query<Job> stillRunningJobsQuery = jobDAO.createQuery().filter("runningTests >", 0);
            QueryResults<Job> runningJobs = jobDAO.find(stillRunningJobsQuery);
            while (runningJobs.asList().size() != 0) {
                logger.info("waiting for all agents to finish running tests, {} jobs are still running:", runningJobs.asList().size());
                logger.info(Arrays.toString(runningJobs.asList().toArray()));
                Thread.sleep(5000);
                runningJobs = jobDAO.find(stillRunningJobsQuery);
            }
        }
        catch (Exception e){
            logger.warn("failed to suspend server", e);
            return Response.serverError().build();
        }
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


    // events

    private void broadcastMessage(String type, Object value) {
        if (value != null) {
            try {
                OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
                OutboundEvent event = eventBuilder.name(type)
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(value.getClass(), value)
                        .build();

                broadcaster.broadcast(event);
            } catch (Throwable ignored) {
                logger.error("Invoking of broadcastMessage() failed due the [{}], type={}, value:{}", ignored.toString(), type, value, ignored);
                ignored.printStackTrace();
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
        logger.warn("Agent {} is did not report on time while he was IDLING and will be deleted", agent);
        final Agent toDelete = agentDAO.findOne(agentDAO.createQuery().field("name").equal(agent.getName()));
        if (toDelete != null) {
            agentDAO.getDatastore().findAndDelete(agentDAO.createIdQuery(toDelete.getId()));
        }
    }

    private void handleUnseenAgent(Agent agent) {
        logger.warn("Agent {} is did not report on time", agent);
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
                logger.warn("test {} was released since agent {} not seen for a long time", found, agent);
                tests.add(found);
            }
        }
        Job job = null;
        if (agent.getJobId() != null) {
            UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations();
            if (agent.getState() == Agent.State.PREPARING) {
                jobUpdateOps.removeAll("preparingAgents", agent.getName());
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(agent.getJobId()), jobUpdateOps);
                logger.info("DEBUG (returnTests) agent is PREPARING ---> update job: [{}]", job);
            } else if (agent.getState() == Agent.State.RUNNING && !tests.isEmpty()) {
                jobUpdateOps.inc("runningTests", 0 - tests.size());
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(agent.getJobId()), jobUpdateOps);
                logger.info("DEBUG (returnTests) agent is running ---> update job: [{}]", job);
                logger.info("DEBUG (returnTests) ---> 0 - tests.size(): [{}]", 0 - tests.size());
            }
        }
        Agent ag = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(agent.getId()),
                agentDAO.createUpdateOperations().set("currentTests", new HashSet<>()).set("state", Agent.State.IDLING));

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


}
