package com.gigaspaces.newman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.*;
import com.gigaspaces.newman.beans.repository.*;
import com.gigaspaces.newman.beans.specification.BuildSpecifications;
import com.gigaspaces.newman.beans.specification.JobSpecifications;
import com.gigaspaces.newman.beans.specification.TestSpecifications;
import com.gigaspaces.newman.config.JpaConfig;
import com.gigaspaces.newman.dto.BuildDTO;
import com.gigaspaces.newman.dto.BuildsComparisonDTO;
import com.gigaspaces.newman.dto.PSuiteDTO;
import com.gigaspaces.newman.entities.*;
import com.gigaspaces.newman.projections.*;
import com.gigaspaces.newman.utils.FileUtils;
import com.gigaspaces.newman.utils.StringUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static com.gigaspaces.newman.beans.specification.JobSpecifications.hasPreparingAgents;

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
    public static final String DELETED_SUITE = "deleted-suite";
    public static final String CREATE_FUTURE_JOB = "created-future-job";
    public static final String DELETED_FUTURE_JOB = "deleted-future-job";
    private static final String MODIFY_SERVER_STATUS = "modified-server-status";

    private int highestPriorityJob;

    private final JobRepository jobRepository;
    private final TestRepository testRepository;
    private final BuildRepository buildRepository;
    private final AgentRepository agentRepository;
    private final SuiteRepository suiteRepository;
    private final PrioritizedJobRepository prioritizedJobRepository;
    private final JobConfigRepository jobConfigRepository;
    private final FutureJobRepository futureJobRepository;
    private final BuildsCacheRepository buildsCacheRepository;
    private static final String SERVER_TESTS_UPLOAD_LOCATION_FOLDER = "tests-logs";
    private static final String SERVER_JOBS_UPLOAD_LOCATION_FOLDER = "job-setup-logs";
    private static final String SERVER_CACHE_BUILDS_FOLDER = "builds";

    @SuppressWarnings("FieldCanBeLocal")
    private final Timer timer = new Timer(true);

    private final ConcurrentHashMap<String, Object> agentLocks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, OfflineAgent> offlineAgents = new ConcurrentHashMap<>();

    private static final int maxJobsPerSuite = 5;

    private final static String CRITERIA_PROP_NAME = "criteria";

    final static String MASTER_BRANCH_NAME = "master";

    private final static boolean buildCacheEnabled = Boolean.getBoolean("newman.server.enabledBuildCache");
    private static String HTTP_WEB_ROOT_PATH;
    private static String HTTPS_WEB_ROOT_PATH;

    private static final Object subscribeToJobLock = new Object();
    private static final Object takenTestLock = new Object();
    private static final Object changeJobPriorityLock = new Object();
    private final AtomicLong latestLogSize = new AtomicLong(0);
    private final AtomicLong lastLogSizeCheckTime = new AtomicLong(0);


    private final Object serverStatusLock = new Object();
    private ServerStatus serverStatus = new ServerStatus(ServerStatus.Status.RUNNING);
    private Thread serverSuspendThread;

    private final ApplicationContext context;

    public NewmanResource(@Context ServletContext servletContext) {
        this.context = new AnnotationConfigApplicationContext(JpaConfig.class);

        // Initialize DAOs with the Datastore
        jobRepository = context.getBean(JobRepository.class);
        testRepository = context.getBean(TestRepository.class);
        buildRepository = context.getBean(BuildRepository.class);
        agentRepository = context.getBean(AgentRepository.class);
        suiteRepository = context.getBean(SuiteRepository.class);
        futureJobRepository = context.getBean(FutureJobRepository.class);
        buildsCacheRepository = context.getBean(BuildsCacheRepository.class);
        jobConfigRepository = context.getBean(JobConfigRepository.class);
        prioritizedJobRepository = context.getBean(PrioritizedJobRepository.class);

        highestPriorityJob = getNotPausedHighestPriorityJob();

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
            }, 1000 * 10, 1000 * 10);
        }

        initBuildsCache();

        initWebRootsPath();
    }

    private void handleHangingJob() {
        Job potentialJob = getPotentialJob();
        if (potentialJob == null) {
            // might happened when there are no Jobs that should run (Ready/Running)
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

    public void updateBrokenJob(Job potentialJob) {
        potentialJob.setState(State.BROKEN);
        updateJob(potentialJob.getId(), potentialJob);
        if (potentialJob.getPriority() > 0) {
            deletePrioritizedJob(potentialJob);
        }

        Build build = potentialJob.getBuild();
        BuildStatus status = build.getBuildStatus();

        // Increment and decrement values
        status.incBrokenJobs();
        status.decRunningJobs();

        buildRepository.save(build); // persist updated build status
    }

    public boolean handleZombie(Job potentialJob) {
        if (potentialJob.getLastTimeZombie() == null) {
            if (isZombie(potentialJob)) { // job not running and zombie
                potentialJob.setLastTimeZombie(new Date()); // Set the timestamp for when it became a zombie
                jobRepository.save(potentialJob); // Persist the updated job entity
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
        return findJob(allRequirements(), null, null);
    }

    private Set<String> allRequirements() {
        Set<String> allRequirements = suiteRepository.findAllRequirements();

        return allRequirements;
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
        Iterable<Agent> agents = agentRepository.findAll(); // Retrieve all agents from DB
        // check if there is an agent in the system that can execute this job
        for (Agent agent : agents) {
            boolean hasMatchingCapabilities = (job.getSuite().getRequirements() == null
                                               || job.getSuite().getRequirements().isEmpty()
                                               || agent.getCapabilities().containsAll(job.getSuite().getRequirements()));

            boolean hasMatchingAgentGroup = (job.getAgentGroups() == null
                                             || job.getAgentGroups().isEmpty()
                                             || job.getAgentGroups().contains(agent.getGroupName()));

            if (hasMatchingCapabilities && hasMatchingAgentGroup) {
                return false; // Found matching agent, so not a zombie
            }
        }
        return true;
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
        long buildsCacheCount = buildsCacheRepository.count(); // Assuming there's an ID for the cache, adjust as necessary
        if (buildsCacheCount == 0) {
            buildsCacheRepository.save(new BuildsCache());
        }
    }

    @GET
    @Path("update-sha")
    @Produces(MediaType.TEXT_PLAIN)
    public String updateSha() {
        logger.info("updating SHA for all tests ...");
        int records = 0;
        // Find all tests
        Iterable<Test> tests = testRepository.findAll();

        for (Test test : tests) {
            if (test.getSha() == null) {
                Job job = getJob(test.getJobId());
                // Compute the SHA
                String sha = Sha.compute(test.getName(), test.getArguments(), job.getSuite().getId(), job.getBuild().getBranch());
                // Update the SHA in the Test entity
                test.setSha(sha);
                testRepository.save(test);  // Persist the updated Test entity

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

        List<Job> jobs = retrieveJobs(buildId, orderBy, all, offset, limit); // jobThin=true
        List<JobView> jobViews = new ArrayList<>(jobs.size());

        for (Job job : jobs) {
            jobViews.add(new JobView(job));
        }
        return new Batch<>(jobViews, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("all-builds-and-suites")
    @Produces(MediaType.APPLICATION_JSON)
    public AllSuitesAndBuildsView getAllBuildsAndSuites() {

        List<PSuiteThin> suites = getThinSuites(true);
        List<SuiteView> suiteViews = new ArrayList<>(suites.size());
        for (PSuiteThin suite : suites) {
            suiteViews.add(new SuiteView(suite));
        }

        List<PBuildThin> buildProjections = getThinBuilds();
        List<BuildView> buildViews = new ArrayList<>(buildProjections.size());
        for (PBuildThin buildProjection : buildProjections) {
            buildViews.add(new BuildView(buildProjection));
        }
        return new AllSuitesAndBuildsView(buildViews, suiteViews);
    }

    private List<PBuildThin> getThinBuilds() {
        final int buildsLimit = 30;
        Pageable pageable = PageRequest.of(0, buildsLimit, Sort.by(Sort.Order.desc("buildTime")));

        return buildRepository.findThinBuilds(pageable).getContent();
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

        List<Job> jobs = retrieveJobs(buildId, orderBy, all, offset, limit); // jobThin=false
        return new Batch<>(jobs, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("job/running")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRunningJobs() {
        Long jobCount = jobRepository.countReadyAndRunningJobs();
        return jobCount.toString();
    }

    private List<Job> retrieveJobs(String buildId, List<String> orderBy, boolean all, int offset, int limit) {
        Sort sort = convertOrderByToSort(orderBy);
        // Pageable for pagination (if not 'all')
        Pageable pageable = all
                ? Pageable.unpaged() // If 'all' is true, no limit, fetch all
                : convertOffsetLimitToPage(offset, limit, sort);

        return (buildId != null)
                ? jobRepository.findByBuildId(buildId, pageable).getContent()
                : jobRepository.findAll(pageable).getContent();
    }

    @GET
    @Path("futureJob")
    @Produces(MediaType.APPLICATION_JSON)
    public FutureJob getAndDeleteFutureJob(@Context UriInfo uriInfo) {
        checkServerStatus();

        Optional<FutureJob> opFutureJob = futureJobRepository.findFirstByOrderBySubmitTimeAsc();
        if (opFutureJob.isPresent()) {
            FutureJob futureJob = opFutureJob.get();
            futureJobRepository.deleteById(futureJob.getId());

            broadcastMessage(DELETED_FUTURE_JOB, futureJob);

            return futureJob;
        }

        return null;
    }

    @DELETE
    @Path("deleteFutureJob/{futureJobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFutureJob(final @PathParam("futureJobId") String futureJobId) {
        Optional<FutureJob> opFutureJob = futureJobRepository.findById(futureJobId);

        if (opFutureJob.isPresent()) {
            FutureJob futureJob = opFutureJob.get();
            futureJobRepository.deleteById(futureJob.getId());

            broadcastMessage(DELETED_FUTURE_JOB, futureJob);
        }

        return Response.ok(Entity.json(futureJobId)).build();
    }

    @GET
    @Path("job/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job getJob(@PathParam("id") final String id) {
        Job job = null;
        Optional<Job> opJob = jobRepository.findById(id);
        if (opJob.isPresent()) {
            job = opJob.get();

            long filterTestsStart = System.currentTimeMillis();
            // Fetch distinct agents assigned to the job from Test entities
            Set<String> agents = testRepository.findDistinctAssignedAgentByJobId(id);
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
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String deleteJobUntilDesiredSpace(final @PathParam("nubmerOfDaysToNotDelete") String nubmerOfDaysToNotDelete) {
        int numberOfDays = Integer.parseInt(nubmerOfDaysToNotDelete);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -numberOfDays);
        Date deleteUntilDate = cal.getTime();
        logger.info("Delete builds and jobs from the last " + numberOfDays + " days, until date: " + deleteUntilDate);

        //stats
        int jobsDeleted = 0;
        int buildsDeleted = 0;

        // delete old jobs
        // Get jobs that should be deleted (submitTime < deleteUntilDate and state != RUNNING)
        List<Job> jobs = jobRepository.findBySubmitTimeBeforeAndStateNot(deleteUntilDate, State.RUNNING);    // find ALL with 'submitTime' less than 'deleteUntilDate' and not 'RUNNING'
        for (Job job : jobs) {
            deleteJob(job.getId());   // delete JOB
            jobsDeleted++;

            buildRepository.deleteById(job.getBuild().getId());  // delete related BUILD as well
            buildsDeleted++;

            if (logger.isDebugEnabled()) {
                logger.debug("deleted job: " + job.getId() + " with build time " + job.getBuild().getBuildTime());
            }
        }

        //delete old builds without jobs
        List<Build> buildList = buildRepository.findByBuildTimeBefore(deleteUntilDate);
        for (Build build : buildList) {
            List<Job> jobList = jobRepository.findAllByBuildId(build.getId());
            if (jobList.isEmpty()) {
                logger.debug("No jobs for build, delete build: " + build.getId() + " with build time: " + build.getBuildTime());
                buildRepository.deleteById(build.getId());
                buildsDeleted++;
            }
        }

        return "Builds deleted: " + buildsDeleted + ", Jobs deleted: " + jobsDeleted
                + " - from the last " + numberOfDays + " days, until date: " + deleteUntilDate;
    }


    @PUT
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createJob(JobRequest jobRequest, @Context SecurityContext sc) {
        checkServerStatus();

        Optional<Build> opBuild = buildRepository.findById(jobRequest.getBuildId());
        if (!opBuild.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Optional<Suite> opSuite = Optional.empty();
        if (jobRequest.getSuiteId() != null) {
            opSuite = suiteRepository.findById(jobRequest.getSuiteId());
        }

        if (!opSuite.isPresent()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("invalid suite id for Job request: " + jobRequest).build();
        }

        Optional<JobConfig> opJobConfig = jobConfigRepository.findById(jobRequest.getConfigId());

        Job job = new Job();
        job.setBuild(opBuild.get());
        job.setSuite(opSuite.get());
        job.setState(State.READY);
        job.setSubmitTime(new Date());
        job.setSubmittedBy(jobRequest.getAuthor());
        job.setAgentGroups(jobRequest.getAgentGroups());
        job.setPriority(jobRequest.getPriority());
        if (opJobConfig.isPresent()) {
            job.setJobConfig(opJobConfig.get());
        }
        job = jobRepository.save(job);
        if (job.getPriority() > 0) {
            createPrioritizedJob(job);
        }

        // update build
        Build build = opBuild.get();
        build.getBuildStatus().getSuites().add(new BuildStatusSuite(opSuite.get().getId(), opSuite.get().getName()));
        build.getBuildStatus().incTotalJobs();
        build.getBuildStatus().incPendingJobs();

        buildRepository.save(build);

        broadcastMessage(CREATED_JOB, job);
        broadcastMessage(MODIFIED_BUILD, build);
        return Response.ok(job).build();
    }

    private void createPrioritizedJob(Job job) {
        PrioritizedJob prioritizedJob = new PrioritizedJob(job);
        prioritizedJobRepository.save(prioritizedJob);
        if (job.getPriority() > highestPriorityJob) {
            highestPriorityJob = job.getPriority();
            logger.info("Highest Priority of jobs changed to: " + highestPriorityJob);
        }
    }

    @POST
    @Path("futureJob")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<FutureJob> createFutureJobs(FutureJobsRequest futureJobsRequest, @Context SecurityContext sc) {
        checkServerStatus();

        String authorOpt = futureJobsRequest.getAuthor();
        String author = (authorOpt != null && authorOpt.length() > 0 ? authorOpt : sc.getUserPrincipal().getName());
        Build build = null;
        JobConfig jobConfig = null;
        List<String> suites = futureJobsRequest.getSuites();
        String buildId = futureJobsRequest.getBuildId();
        String configId = futureJobsRequest.getConfigId();
        Set<String> agentGroups = futureJobsRequest.getAgentGroups();
        int priority = futureJobsRequest.getPriority();

        if (buildId != null) {
            Optional<Build> opBuild = buildRepository.findById(buildId);
            build = opBuild.orElseThrow(() -> new BadRequestException("invalid build id in create FutureJob: " + buildId));

        }
        if (configId != null) {
            Optional<JobConfig> opJobConfig = jobConfigRepository.findById(configId);
            jobConfig = opJobConfig.orElseThrow(() -> new BadRequestException("invalid config id in create FutureJob: " + configId));
        }
        List<FutureJob> response = new ArrayList<>();
        if (!suites.isEmpty()) {
            for (String suiteId : suites) {
                Suite suite = suiteRepository.findById(suiteId).orElseThrow(()
                        -> new BadRequestException("invalid suite id in create FutureJob: " + suiteId));

                //noinspection ConstantConditions
                FutureJob futureJob = new FutureJob(build.getId(), build.getName(), build.getBranch(),
                        suite.getId(), suite.getName(), jobConfig.getId(), jobConfig.getName(),
                        author, agentGroups, priority);
                response.add(futureJob);

                futureJobRepository.save(futureJob);
                broadcastMessage(CREATE_FUTURE_JOB, futureJob);

            }
        }
        return response;
    }


    @POST
    @Path("job/{id}/toggle")
    @Produces(MediaType.APPLICATION_JSON)
    public Job toggelJobPause(@PathParam("id") final String id) {
        Optional<Job> opJob = jobRepository.findById(id);
        if (opJob.isPresent()) {
            Job job = opJob.get();
            State state = null;
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
                State old = job.getState(); // TODO consider if that is needed
                job.setState(state);

                if (state == State.PAUSED) {
                    // remove startPrepareTime after turn job to paused because after pause agents do setup again on job
                    job.setStartPrepareTime(null);
                } else if (state == State.READY) {  //change status of Test(s) from running to pending
                    int updatedCount = testRepository.updateStatusByJobIdAndCurrentStatus(id, Test.Status.RUNNING, Test.Status.PENDING);
                    logger.info("---ToggleJobPause, state is READY, affected count:" + updatedCount);
                }

                managePrioritizedJob(job, state == State.PAUSED);
                job = jobRepository.saveAndFlush(job);//.findAndModify(jobRepository.createIdQuery(job.getId()).field("state").equal(old), updateJobStatus);
                broadcastMessage(MODIFIED_JOB, job);

                return job;
            }
        }
        return null;
    }

    private void managePrioritizedJob(Job job, boolean isPaused) {
        if (job.getPriority() > 0) {
            Optional<PrioritizedJob> opPrioritizedJob = prioritizedJobRepository.findByJobId(job.getId());
            PrioritizedJob prioritizedJob = opPrioritizedJob.orElseThrow(() -> new RuntimeException("PrioritizedJob [" + job.getId() + "] does not exist"));;
            prioritizedJob.setPaused(isPaused);

            prioritizedJob = prioritizedJobRepository.save(prioritizedJob);

            boolean priorityConditionTrue = isPaused
                    ? prioritizedJob.getPriority() == highestPriorityJob
                    : prioritizedJob.getPriority() > highestPriorityJob;

            if (priorityConditionTrue) {
                highestPriorityJob = isPaused
                        ? getNotPausedHighestPriorityJob()
                        : prioritizedJob.getPriority();
            }
        }
    }

    @POST
    @Path("jobs/pause")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Job> toggleMultipleJobPause(final List<String> ids) {
        List<Job> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Optional<Job> opJob = jobRepository.findById(id);
            if (opJob.isPresent()) {
                Job job = opJob.get();
                State state = null;
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
                if (state == State.PAUSED) {
                    State old = job.getState(); // TODO consider if that is needed
                    job.setState(state);
                        // remove startPrepareTime after turn job to paused because after pause agents do setup again on job
                    job.setStartPrepareTime(null);
                    if (job.getPriority() > 0) {
                        Optional<PrioritizedJob> opPrioritizedJob = prioritizedJobRepository.findByJobId(job.getId());
                        PrioritizedJob prioritizedJob = opPrioritizedJob.orElseThrow(() -> new RuntimeException("PrioritizedJob [" + id + "] does not exist"));;
                        prioritizedJob.setPaused(true);

                        prioritizedJob = prioritizedJobRepository.save(prioritizedJob);
                        if (prioritizedJob.getPriority() == highestPriorityJob) {
                            highestPriorityJob = getNotPausedHighestPriorityJob();
                        }
                    }
                    job = jobRepository.save(job); //jobRepository.getDatastore().findAndModify(jobRepository.createIdQuery(job.getId()).field("state").equal(old), updateJobStatus);
                    result.add(job);
                    broadcastMessage(MODIFIED_JOB, job);
                }
            }
        }
        return result;
    }

    @POST
    @Path("job/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job AgentFinishJob(@PathParam("jobId") final String jobId) {
        int updated = jobRepository.updateJobToReadyIfDone(jobId);
        if (updated > 0) {
            Job updatedJob = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job [" + jobId + "] does not exist"));;
            broadcastMessage(MODIFIED_JOB, updatedJob);

            return updatedJob;
        }

        return null;
    }

    @POST
    @Path("jobs/resume")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Job> toggelMultipleJobResume(final List<String> ids) {
        List<Job> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Optional<Job> opJob = jobRepository.findById(id);
            if (opJob.isPresent()) {
                Job job = opJob.get();
                State state = null;
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
                    State old = job.getState(); // TODO consider if this is needed
                    job.setState(state); // .findAndModify(jobDAO.createIdQuery(job.getId()).field("state").equal(old), updateJobStatus);
                    result.add(job);
                    if (job.getPriority() > 0) {
                        Optional<PrioritizedJob> opPrioritizedJob = prioritizedJobRepository.findByJobId(job.getId());
                        if (!opPrioritizedJob.isPresent()) {
                            return null;
                        }
                        PrioritizedJob prioritizedJob = opPrioritizedJob.get();
                        prioritizedJob.setPaused(false);
                        if (prioritizedJob.getPriority() > highestPriorityJob) {
                            highestPriorityJob = prioritizedJob.getPriority();
                        }
                    }
                    broadcastMessage(MODIFIED_JOB, job);

                    //change status of Test(s) from RUNNING to PENDING
                    int updatedCount = testRepository.updateStatusByJobIdAndCurrentStatus(id, Test.Status.RUNNING, Test.Status.PENDING);
                    logger.info("---toggelMultipleJobResume, state is READY, affected count:" + updatedCount);
                }
            }
        }
        return result;
    }

    @GET
    @Path("dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardData getActiveJobGroup(@Context UriInfo uriInfo) {

//        addRequiredBuildTableColumns(activeBuildsQuery);
        List<Build> activeBuilds = buildRepository.findActiveBuildsDescByBuildTime();
        activeBuilds.stream().parallel().peek(Build::excludeFields);

//        addRequiredBuildTableColumns(pendingBuildsQuery);
        Pageable pageable = PageRequest.of(0, 5);
        List<Build> pendingBuilds = buildRepository.findPendingBuildsDescByBuildTime(pageable);
        pendingBuilds.stream().parallel().peek(Build::excludeFields);

//        addRequiredBuildTableColumns(historyBuildsQuery);
        pageable = PageRequest.of(0, 5);
        List<Build> historyBuilds = buildRepository.findRecentlyCompletedBuildsDescByBuildTime(pageable);
        historyBuilds.stream().parallel().peek(Build::excludeFields);

        Map<String, List<PJobForDashboard>> activeJobsMap = createActiveJobsMap(activeBuilds);

        return new DashboardData(activeBuilds, pendingBuilds, historyBuilds, activeJobsMap, futureJobRepository.findAll());
    }

    @GET
    @Path("build/{build1Date}/compare/{build2Date}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildsComparisonDTO getJobRunsComparison(@PathParam("build1Date") String build1DateStr, @PathParam("build2Date") String build2DateStr ) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startDateLeft = formatter.parse(build1DateStr + " 17:00:00");
        Date endDateLeft = new Date(startDateLeft.getTime() + TimeUnit.MINUTES.toMillis(2));

        Date startDateRight = formatter.parse(build2DateStr + " 17:00:00");
        Date endDateRight = new Date(startDateRight.getTime() + TimeUnit.MINUTES.toMillis(2));

        BuildsComparisonDTO buildsComparisonDTO = new BuildsComparisonDTO();

        List<Job> jobsLeft = jobRepository.findBySubmittedByAndSubmitTime("root", startDateLeft, endDateLeft);
        List<Job> jobsRight = jobRepository.findBySubmittedByAndSubmitTime("root", startDateRight, endDateRight);

        if (!jobsLeft.isEmpty()) {
            Build buildLeft = jobsLeft.get(0).getBuild();
            buildsComparisonDTO.setBuildLeftDetails(new String[]{buildLeft.getId(), buildLeft.getName()});
            for (Job job : jobsLeft) {
                buildsComparisonDTO.addSuiteLeft(job.getSuite().getName(), job.getId(),
                        job.getTotalTests(), job.getPassedTests(), job.getFailedTests(), job.getFailed3TimesTests());
            }
        }

        if (!jobsRight.isEmpty()) {
            Build buildRight = jobsRight.get(0).getBuild();
            buildsComparisonDTO.setBuildRightDetails(new String[]{buildRight.getId(), buildRight.getName()});
            for (Job job : jobsRight) {
                buildsComparisonDTO.addSuiteRight(job.getSuite().getName(), job.getId(),
                        job.getTotalTests(), job.getPassedTests(), job.getFailedTests(), job.getFailed3TimesTests());
            }
        }

        return buildsComparisonDTO;
    }


    @POST
    @Path("unsubscribe")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job unsubscribe(final Agent agent) {
        String jobId = agent.getJobId();
        if (jobId == null) {
            throw new BadRequestException("can't unsubscribe agent without a job " + agent);
        }

        Job job = null;
        if (agent.getState() == Agent.State.PREPARING) {
            Optional<Job> opJob = jobRepository.findById(jobId);
            job = opJob.orElseThrow(() -> new RuntimeException("Job [" + jobId + "] does not exist"));;
            job.getPreparingAgents().remove(agent.getName());

            job = jobRepository.save(job);
            broadcastMessage(MODIFIED_JOB, job);
        }

        agentRepository.unsetJobIdByName(agent.getName());
        broadcastMessage(MODIFIED_AGENT, agent);
        return job;
    }

    @POST
    @Path("freeAgent/{agentName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Agent freeAgent(@PathParam("agentName") final String agentName) {
        Optional<Agent> opAgent = agentRepository.findByName(agentName);
        if (opAgent.isPresent()) {
            returnTests(opAgent.get());
            handleZombieAgent(opAgent.get());
        }
        return opAgent.orElse(null);
    }

    @PUT
    @Path("tests")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addTests(Batch<Test> tests, @QueryParam("toCount") String toCountStr) {
        if (tests.getValues().isEmpty()) {
            return;
        }
        Optional<Job> opExistingJob = jobRepository.findById(tests.getValues().get(0).getJobId());
        if (!opExistingJob.isPresent()) {
            return;
        }

        Build build = null;
        Job job = opExistingJob.get();
        int updateNumOfTestRetries = 0;

        List<Test> res = new ArrayList<>(tests.getValues().size());
        for (Test test : tests.getValues()) {
            res.add(addTest(test, job));    // first time test added with PENDING status
            if ((test.getRunNumber() > 1)) {
                ++updateNumOfTestRetries;
            }
        }

        if (!res.isEmpty()) {
            Test test = res.get(0);

            Optional<Job> opJob = jobRepository.findById(test.getJobId());
            Optional<Build> opBuild = buildRepository.findById(job.getBuild().getId());
            final String buildId = job.getBuild().getId();
            job = opJob.orElseThrow(() -> new RuntimeException("Job [" + test.getJobId() + "] does not exist"));;
            build = opBuild.orElseThrow(() -> new RuntimeException("Build [" + buildId + "] does not exist"));;

            // @QueryParam("toCount") - indicates if tests should be added to "totalTests"
            // if a test fails and is resumitted (aka run number >1 ) we don't want it to be added to "totalTests".
            if (toCountStr.equals("count")) {
                job.setTotalTests(job.getTotalTests() + res.size());
                build.getBuildStatus().setTotalTests(build.getBuildStatus().getTotalTests() + res.size());
            }

            if (updateNumOfTestRetries > 0) {
                job.setNumOfTestRetries(job.getNumOfTestRetries() + updateNumOfTestRetries);
                build.getBuildStatus().setNumOfTestRetries(build.getBuildStatus().getNumOfTestRetries() + updateNumOfTestRetries);
            }

            if (toCountStr.equals("count") || updateNumOfTestRetries > 0) {
                job = jobRepository.save(job);
                build = buildRepository.save(build);

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
            HashSet<String> excludeTags = new HashSet<>(Arrays.asList("RELEASE"));
            Specification<Build> spec = Specification.where(
                    BuildSpecifications.matchesBranch(branchStr))     // .field("branch").equal(branchStr)
                    .and(BuildSpecifications.hasNoTags(excludeTags))  // .field("tags").hasNoneOf(excludeTags);
                    .and(BuildSpecifications.hasTags(tagsSet))        // .field("tags").hasAllOf(tagsSet);
                    .and(BuildSpecifications.hasNoJobsInBuildStatus()); //  build.getBuildStatus().getTotalJobs() == 0

            Page<Build> page = buildRepository.findAll(spec, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "buildTime"))); // .order("-buildTime")
            Optional<Build> opBuild = page.getContent().stream().findFirst();
            if (opBuild.isPresent()) {
                buildsRes = opBuild.get();
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
        List<Build> builds = buildRepository.findBuildsWithTags("RELEASE", "XAP");
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

    private Test addTest(Test test, Job job) {
        if (test.getJobId() == null) {
            throw new BadRequestException("can't add test with no jobId: " + test);
        }
        test.setStatus(Test.Status.PENDING);
        test.setScheduledAt(new Date());
        test.setSha(Sha.compute(test.getName(), test.getArguments(), job.getSuite().getId(), job.getBuild().getBranch()));
        if (test.getLogs() == null) {
            test.setLogs(new TestLog(test));
        } else if (test.getLogs().getTest() == null) {
            test.getLogs().setTest(test);
        }
        test = testRepository.save(test);

        broadcastMessage(CREATED_TEST, test);
        return test;
    }


    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public synchronized Test finishTest(final Test test) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("trying to finish test - id:[{}], name:[{}]", test.getId(),
                        test.getName());
            }
            if (test.getId() == null) {
                throw new BadRequestException("can't finish test without testId: " + test);
            }

            // LOCK job when manipulating with its counters and statuses
            Job testJob = getJob(test.getJobId());
            if (testJob == null) {
                throw new BadRequestException("finishTest - the job of the test is not on database. Test: [" + test + "].");
            }
            Test.Status status = test.getStatus();
            if (status == null || (status != Test.Status.FAIL && status != Test.Status.SUCCESS)) {
                throw new BadRequestException("can't finish test without state set to success or fail state" + test);
            }
            // find TEST
            Optional<Test> opTest = testRepository.findByIdAndStatusNot(test.getId(), status);
            if (!opTest.isPresent()) {
                return null;    // if no test with different status found - nothing to change then because it was already changed
            }
            Test existingTest = opTest.get();
            if (test.getErrorMessage() != null) {
                existingTest.setErrorMessage(test.getErrorMessage());
            }
            existingTest.setEndTime(new Date());
            existingTest.setStatus(status);

            int historyLength = 25;
            List<TestHistoryItem> testHistory = getTests(test.getId(), 0, historyLength, null).getValues();
            String historyStatsString = TestScoreUtils.decodeShortHistoryString(test, testHistory, test.getStatus(), testJob.getBuild()); // added current fail to history;
            double reliabilityTestScore = TestScoreUtils.score(historyStatsString);

            existingTest.setTestScore(reliabilityTestScore);
            existingTest.setHistoryStats(historyStatsString);

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "got test history [{}] of test and prepare to update:  id:[{}], name:[{}], jobId:[{}], running tests before decrement:[{}]",
                        historyStatsString, test.getId(), test.getName(), test.getJobId(),
                        testJob.getRunningTests());
            }
            // save updated TEST
            Test savedTest = testRepository.saveAndFlush(existingTest); // SAVE Test

            // Now, update TEST related things
            Job updateJobStatus = jobRepository.findById(existingTest.getJobId()).get(); //existingTest.getJobId()
            Build updateBuild = buildRepository.findById(updateJobStatus.getBuild().getId()).get(); //job.getBuild().getId())
            if (test.getRunNumber() == 1) {
                if (status == Test.Status.FAIL) {
                    updateJobStatus.incFailedTests();
                    updateBuild.getBuildStatus().incFailedTests();
                } else {
                    updateJobStatus.incPassedTests();
                    updateBuild.getBuildStatus().incPassedTests();
                }
            } else if (test.getRunNumber() == 3 && status == Test.Status.FAIL) {
                updateJobStatus.incFailed3Tests();
                updateBuild.getBuildStatus().incFailed3Tests();
            }

            updateJobStatus.decRunningTests();
            updateBuild.getBuildStatus().decRunningTests();

            boolean testEntryExists = testRepository.existsByJobIdAndStatusIn(existingTest.getJobId(), Arrays.asList(Test.Status.PENDING, Test.Status.RUNNING));
            if (!testEntryExists) {
                updateJobStatus.setState(State.DONE).setEndTime(new Date());
                updateBuild.getBuildStatus().incDoneJobs().decRunningJobs();
                if (testJob.getPriority() > 0) {
                    deletePrioritizedJob(testJob);
                }
            }

            Job savedJob = jobRepository.saveAndFlush(updateJobStatus);     // SAVE Job
            if (savedJob.getRunningTests() < 0) {
                logger.warn("Job: " + savedJob.getId() + " has an illegal number of running tests: " + savedJob.getRunningTests() +
                        " after running test: " + test.getArguments() + " with agent: " + test.getAssignedAgent());
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "After modifying job ( after runningTests decrement ), runningTests:[{}]",
                        savedJob.getRunningTests());
            }

            Build savedBuild = buildRepository.saveAndFlush(updateBuild);   // SAVE Build

            broadcastMessage(MODIFIED_BUILD, savedBuild);
            broadcastMessage(MODIFIED_TEST, savedTest);
            broadcastMessage(MODIFIED_JOB, savedJob);

            logger.info("succeed finish test- id:[{}], name:[{}]", savedTest.getId(), savedTest.getName());
            return savedTest;
        } catch (Exception e) {
            logger.error("failed to finish test because: ", e);
            throw e;
        } finally {
            if (test.getAssignedAgent() != null) {
                Optional<Agent> opAgent = agentRepository.findByName(test.getAssignedAgent());
                if (opAgent.isPresent()) {
                    Agent currAssignedAgent = opAgent.get();
                    currAssignedAgent.setLastTouchTime(new Date());
                    currAssignedAgent.getCurrentTests().remove(test.getId());   // remove assigned tests one by one when they complete

                    if (currAssignedAgent.getCurrentTests().isEmpty()) {
                        currAssignedAgent.setState(Agent.State.IDLING);     // set agent back to IDLING if it doesn't have tasks to run
                    }

                    agentRepository.saveAndFlush(currAssignedAgent);
                    broadcastMessage(MODIFIED_AGENT, currAssignedAgent);

                    if (currAssignedAgent.getState() == Agent.State.IDLING) {
                        logger.debug("agent [{}] become idling because it finish all his tests", currAssignedAgent.getName());
                    }
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

        List<PTest> tests = retrieveJobTests(jobId, orderBy, all, offset, limit);
        List<TestView> testsView = new ArrayList<>(tests.size());
        for (PTest PTest : tests) {
            testsView.add(new TestView(PTest));
        }

        return new Batch<>(testsView, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion() {
        return Response.ok(BuildVersionProvider.getVersion()).build();
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<TestView> getJobTests(@DefaultValue("0") @QueryParam("offset") int offset,
                                   @DefaultValue("30") @QueryParam("limit") int limit,
                                   @DefaultValue("false") @QueryParam("all") boolean all,
                                   @QueryParam("orderBy") List<String> orderBy,
                                   @QueryParam("jobId") String jobId,
                                   @Context UriInfo uriInfo) {

        List<PTest> tests = jobId != null && jobId.trim().length() > 0
                ? retrieveJobTests(jobId, orderBy, all, offset, limit)
                : Collections.emptyList();

        List<TestView> testsView = new ArrayList<>(tests.size());
        for (PTest PTest : tests) {
            testsView.add(new TestView(PTest));
        }

        return new Batch<>(testsView, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("agent-tests")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<TestView> getAgentTests(@DefaultValue("0") @QueryParam("offset") int offset,
                                     @DefaultValue("30") @QueryParam("limit") int limit,
                                     @DefaultValue("false") @QueryParam("all") boolean all,
                                     @QueryParam("orderBy") List<String> orderBy,
                                     @QueryParam("agentName") String agentName,
                                     @Context UriInfo uriInfo) {

        List<PTest> tests = (agentName != null && agentName.trim().length() > 0)
                ? retrieveAgentTests(agentName, orderBy, all, offset, limit)
                : Collections.emptyList();

        List<TestView> testsView = new ArrayList<>(tests.size());
        for (PTest PTest : tests) {
            testsView.add(new TestView(PTest));
        }

        return new Batch<>(testsView, offset, limit, all, orderBy, uriInfo);
    }

    private List<PTest> retrieveJobTests(String jobId, List<String> orderBy, boolean all, int offset, int limit) {
        Pageable pageable = preparePageable(orderBy, all, offset, limit);
        return testRepository.findAllByJobId(jobId, pageable);
    }

    private List<PTest> retrieveAgentTests(String agentName, List<String> orderBy, boolean all, int offset, int limit) {
        Pageable pageable = preparePageable(orderBy, all, offset, limit);
        return testRepository.findAllByAssignedAgent(agentName, pageable);
    }

    private Pageable preparePageable(List<String> orderBy, boolean all, int offset, int limit) {
        Sort sort = Sort.unsorted();
        if (orderBy != null) {
            if (orderBy.isEmpty()) {
                orderBy.add("startTime");
            }
            sort = convertOrderByToSort(orderBy);
        }

        // Handle pagination
        return all ? Pageable.unpaged() : convertOffsetLimitToPage(offset, limit, sort);
    }

    private Sort convertOrderByToSort(List<String> orderBy) {
        if (orderBy == null || orderBy.isEmpty()) {
            return Sort.unsorted();
        }
        return Sort.by(orderBy.stream()
                .map(field -> field.startsWith("-")
                        ? Sort.Order.desc(field.substring(1))
                        : Sort.Order.asc(field))
                .collect(Collectors.toList()));
    }

    private Pageable convertOffsetLimitToPage(int offset, int limit, Sort sort) {
        if (sort == null) {
            sort = Sort.unsorted();
        }
        // Pageable for pagination (if not 'all')
        return PageRequest.of(offset / limit, limit, sort);
    }

    @GET
    @Path("test/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("id") String id) {
        Optional<Test> testOptional = testRepository.findById(id);
        return testOptional.orElse(null);
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
        logger.info("Received log file for jobId:[{}], id:[{}]", jobId, id);
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
            Optional<Job> opJob = jobRepository.findById(jobId);
            if (opJob.isPresent()) {
                saveFile(fileInputStream, filePath);
                URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();

                Job job = opJob.get();

                String jobSetupLogsValue = uri.toASCIIString();
                job.getJobSetupLog().getAgentLogs().put(agentName, jobSetupLogsValue);

                job = jobRepository.save(job);
                broadcastMessage(MODIFIED_JOB, job);
            }
        } catch (Exception e) {
            logger.error("Failed to save log at {} for jobId {}", filePath, jobId, e);
        }
    }

    private void handleTestLogFile(String testId, String jobId, UriInfo uriInfo, InputStream fileInputStream, String fileName) {
        String filePath = calculateTestLogFilePath(jobId, testId) + fileName;
        logger.info("Test log file path calculated: " + filePath);
        try {
            Optional<Test> opTest = testRepository.findById(testId);
            if (opTest.isPresent()) {
                saveFile(fileInputStream, filePath);
                logger.info("Log file saved on path: " + filePath);
                URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();

                Test test = opTest.get();
                // Update the map
                String jobSetupLogsValue = uri.toASCIIString();
                test.getLogs().getTestLogs().put(fileName, jobSetupLogsValue);
                logger.info("Test logs added to the test {}:  {}", test.getId(), test.getLogs().getTestLogs());

                test = testRepository.save(test);
                broadcastMessage(MODIFIED_TEST, test);
            }
        } catch (Exception e) {
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
            String uri = uriInfo.getAbsolutePathBuilder().path(fileName).build().getPath();

            Optional<Test> optionalTest = testRepository.findById(testId);
            if (optionalTest.isPresent()) {
                Test test = optionalTest.get();
                for (String entry : entries) {
                    test.getLogs().getTestLogs().put(entry, uri + "!/" + entry);
                }

                test = testRepository.save(test);
                broadcastMessage(MODIFIED_TEST, test);
            }
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
            Optional<Build> opBuild = buildRepository.findById(buildIdToCache);
            Build build = opBuild.orElseThrow(() ->
                    new IllegalArgumentException("cant cache the build with id " + buildIdToCache + " because it does not exists"));

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
        buildsCacheRepository.updateBuildCache(cache.getId(), cache.getCache(), cache.getIndex(), cache.getSize());
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
        BuildsCache cache = buildsCacheRepository.findTopBy();
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
        long count = agentRepository.count();
        if (logger.isDebugEnabled()) {
            logger.debug("agents count=" + count);
        }
        return Response.ok(count, MediaType.TEXT_PLAIN_TYPE).build();
    }

    @GET
    @Path("agents/failing")
    public Response getFailingAgents() {
        long count = agentRepository.countBySetupRetriesGreaterThan(0);
        if (logger.isDebugEnabled()) {
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

        Sort sort = convertOrderByToSort(orderBy);
        Pageable pageable = convertOffsetLimitToPage(offset, limit, sort);

        return new Batch<>(agentRepository.findAll(pageable).getContent(), offset, limit,
                false, orderBy, uriInfo);
    }

    @GET
    @Path("ping/{name}/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String pingInSetup(@PathParam("name") final String name, @PathParam("jobId") final String jobId) {
        Optional<Agent> opAgent = agentRepository.findByName(name);
        if (!opAgent.isPresent()) {
            logger.error("Unknown agent " + name);
            return null;
        }

        Agent agent = opAgent.get();
        agent.setLastTouchTime(new Date());

        agent = agentRepository.save(agent);

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
        Optional<Agent> opAgent = agentRepository.findByName(name);
        //if such agent not found (already does not exist), then create Agent instance and
        //provide only its name
        return opAgent.orElseGet(() -> {
            Agent a = new Agent();
            a.setName(name);
            return a;
        });
    }

    @GET
    @Path("agent")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Agent> getAgents(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("30") @QueryParam("limit") int limit,
                                  @DefaultValue("false") @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

        Sort sort = convertOrderByToSort(orderBy);
        // Pageable for pagination (if not 'all')
        Pageable pageable = all
                ? Pageable.unpaged() // If 'all' is true, no limit, fetch all
                : PageRequest.of(offset / limit, limit, sort);  // Pagination

        List<Agent> agents = agentRepository.findAll(pageable).getContent();
        for (Agent agent : agents) {
            String jobId = agent.getJobId();    // contains jobId only, Job is transient
            if (jobId != null) {
                PJobThin jobThin = jobRepository.findOneThinJobById(jobId);    // now look for the projection of the related job
                if (logger.isDebugEnabled()) {
                    logger.debug("within for on agents, jobID=" + jobId + ", job=" + jobThin.string());
                }
                agent.setJob(new Job(jobThin.getId(), jobThin.getSuiteId(), jobThin.getSuiteId(),
                        jobThin.getBuildId(), jobThin.getBuildName(), jobThin.getBuildBranch()));
            }
        }

        return new Batch<>(agents, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("agent/offline")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Agent> getOfflineAgents() {

        List<Agent> result = new ArrayList<>(offlineAgents.size());
        for (Map.Entry<String, OfflineAgent> entry : offlineAgents.entrySet()) {
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

        Sort sort = convertOrderByToSort(orderBy);
        List<Agent> agents;
        if (all) {
            agents = agentRepository.findAllWithPositiveSetupRetries(sort);
        } else {
            Pageable pageable = convertOffsetLimitToPage(offset, limit, sort);
            agents = agentRepository.findAllWithPositiveSetupRetries(pageable).getContent();
        }

        for (Agent agent : agents) {
            agent.setSetupRetries(0);
            agentRepository.save(agent);
            broadcastMessage(MODIFIED_AGENT, agent);
        }
        broadcastMessage(MODIFIED_FAILING_AGENTS, agents.size());

        return new Batch<>(agents, offset, limit, all, orderBy, uriInfo);
    }

    @POST
    @Path("agent/{name}/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("name") final String agentName, @PathParam("jobId") final String jobId) {
        checkServerStatus();

        Optional<Agent> opAgent = agentRepository.findByName(agentName);
        if (!opAgent.isPresent()) {
            logger.error("Bad request. Unknown agent {}", agentName);
            return null;
        }

        Agent agent = opAgent.get();
        agent.setLastTouchTime(new Date());     // ping agent is necessary

        if (!jobId.equals(opAgent.get().getJobId())) {
            logger.error("Agent agent is not on job {} {} ", jobId, opAgent.get());
            return null;
        }

        Job pj = null;
        final Object agentLock = getAgentLock(agent);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (agentLock) {
            if (agent.getState() == Agent.State.PREPARING) {
                Optional<Job> opJob = jobRepository.findById(jobId);
                if (opJob.isPresent()) {
                    opJob.get().getPreparingAgents().remove(agent.getName());

                    pj = jobRepository.saveAndFlush(opJob.get());
                }
            }
        }

        // LOCK job when manipulating with its counters and statuses
        Test test = null;
        synchronized (takenTestLock) {      // this is the place where Tests assigned to Agents or removed if job PAUSED
            Optional<Test> opTestPending = testRepository.findFirstByJobIdAndStatus(jobId, Test.Status.PENDING);  // find job waiting to run
            if (opTestPending.isPresent()) {
                test = opTestPending.get();
                test.setStatus(Test.Status.RUNNING);    // reserve test by changing status to RUNNING

                test = testRepository.saveAndFlush(test);
            }
        }

        if (test != null) {
            Optional<Job> opJobNotPaused = jobRepository.findByIdAndStateNot(jobId, State.PAUSED);  // find NOT PAUSED job and set everything RUNNING
            if (opJobNotPaused.isPresent()) {
                // TEST
                test.setStatus(Test.Status.RUNNING);
                test.setAssignedAgent(agentName);
                test.setAgentGroup(agent.getGroupName());
                test.setStartTime(new Date());
                test = testRepository.saveAndFlush(test);   // SAVE test

                // JOB
                Job job = opJobNotPaused.get();
                job.incRunningTests();
                job.setState(State.RUNNING);
                if (job.getStartTime() == null) {
                    job.setStartTime(new Date());
                }
                job = jobRepository.saveAndFlush(job);  // SAVE job

                if (logger.isDebugEnabled()) {
                    logger.debug("After incrementing runningTests for jobId [{}] runningTests [{}]",
                            jobId, job.getRunningTests());
                }

                // BUILD
                final String buildId = job.getBuild().getId();
                Optional<Build> opBuild = buildRepository.findById(job.getBuild().getId());

                Build build = opBuild.orElseThrow(() -> new RuntimeException("Build [" + buildId + "] does not exist"));
                build.getBuildStatus().incRunningTests();
                build.getBuildStatus().incRunningJobs().decPendingJobs();
                build = buildRepository.saveAndFlush(build);    // SAVE build

                logger.info("agent [{}] got test id: [{}], test-state:[{}]", agent.getName(), test.getId(), test.getStatus());

                // AGENT
                agent.addCurrentTest(test.getId());
                agent.setState(Agent.State.RUNNING);
                agent = agentRepository.saveAndFlush(agent);    // SAVE agent

                broadcastMessage(MODIFIED_TEST, test);
                broadcastMessage(MODIFIED_JOB, job);
                broadcastMessage(MODIFIED_BUILD, build);
                broadcastMessage(MODIFIED_AGENT, agent);

                return test;
            } else {
                // if PAUSED job was found - set everything PENDING
                // TEST
                test.setStatus(Test.Status.PENDING);
                test.setAssignedAgent(null);
                test.setStartTime(null);
                test = testRepository.saveAndFlush(test);   // SAVE test

                // AGENT
                agent.getCurrentTests().remove(test.getId());       // take the test assigment away from the agent
                if (agent.getCurrentTests().isEmpty()) {
                    agent.setState(Agent.State.IDLING);
                }

                agent = agentRepository.saveAndFlush(agent);    // SAVE agent

                logger.info("Did not find relevant job, returning the test {} to the pool", test.getId());

                broadcastMessage(MODIFIED_TEST, test);
                broadcastMessage(MODIFIED_AGENT, agent);
                if (pj != null) {
                    broadcastMessage(MODIFIED_JOB, pj);
                }

                if (agent.getState() == Agent.State.IDLING) {
                    logger.warn("agent [{}] is idling from getTest because finished all his tests", agent.getName());
                }

                return null;    // to stop agent going over and over the PAUSED test - return null
            }
        } else {
            logger.info("agent [{}] didn't find ready test for job: [{}]", agent.getName(), jobId);
        }

        agent = agentRepository.saveAndFlush(agent);
        broadcastMessage(MODIFIED_AGENT, agent);

        return null;    // this will stop agent seeking for a pending job
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
        String foundAgentId = null;
        Agent foundAgent = null;
        Optional<Agent> opFoundAgent = agentRepository.findByName(agent.getName());
        // clear older agent data from other jobs.
        if (opFoundAgent.isPresent()) {
            foundAgent = opFoundAgent.get();
            foundAgentId = foundAgent.getId();
            if (foundAgent.getState() == Agent.State.PREPARING) {
                // clear job data if exists.
                if (foundAgent.getJobId() != null && !foundAgent.getJobId().isEmpty()) {
                    final String jobId = foundAgent.getJobId();
                    Optional<Job> opJob = jobRepository.findById(foundAgent.getJobId());
                    Job oldJob = opJob.orElseThrow(() -> new RuntimeException("Job [" + jobId + "] does not exist"));;
                    oldJob.getPreparingAgents().remove(foundAgent.getName());
                    oldJob = jobRepository.save(oldJob); // update old job if agent is not in charge of running it

                    broadcastMessage(MODIFIED_JOB, oldJob);
                }
            } else if (foundAgent.getState() == Agent.State.RUNNING && !foundAgent.getCurrentTests().isEmpty() && foundAgent.getJobId() != null) {
                returnTests(foundAgent);
            }
        }

        Agent newAgent = new Agent();   // create new Agent to subscribe
        newAgent.setId(foundAgentId);
        newAgent.setLastTouchTime(new Date());
        if (agent.getHost() != null) {
            newAgent.setHost(agent.getHost());
            newAgent.setHostAddress(agent.getHostAddress());
            newAgent.setPid(agent.getPid());
            newAgent.setCapabilities(agent.getCapabilities());
        }
        newAgent.setName(agent.getName());
        newAgent.setCurrentTests(new HashSet<>());
        newAgent.setGroupName(agent.getGroupName());
        newAgent.setState(Agent.State.IDLING);

        Agent subscribedAgent;
        Job job;
        synchronized (subscribeToJobLock) { // prevent multiple agents pick up the same job because this awakens multiple agents for just 1 test
            job = findJob(agent.getCapabilities(), JobSpecifications.preparingAgentsCondition(), agent.getGroupName());
            if (job != null) {
                newAgent.setJobId(job.getId());
                newAgent.setState(Agent.State.PREPARING);

                if (job.getPreparingAgents() == null) {
                    job.setPreparingAgents(new HashSet<>());
                }
                job.getPreparingAgents().add(agent.getName());
                if (job.getStartPrepareTime() == null) {
                    logger.info("agent [host:[{}], name:[{}]] start prepare on job [id:[{}], name:[{}]].", newAgent.getHost(), newAgent.getName(), job.getId(), job.getSuite().getName());
                    job.setStartPrepareTime(new Date());
                }
                // job can be run = not a zombie
                if (job.getLastTimeZombie() != null) {
                    job.setLastTimeZombie(null);
                }
                job = jobRepository.save(job);
                broadcastMessage(MODIFIED_JOB, job);
            }

            subscribedAgent = agentRepository.save(newAgent);
        }

        if (foundAgent == null) {
            String agentIp = agent.getHostAddress();
            if (offlineAgents.containsKey(agentIp)) {
                offlineAgents.remove(agentIp);
                broadcastMessage(DELETED_OFFLINE_AGENT, agentIp);
            }
            broadcastMessage(MODIFIED_AGENTS_COUNT, agentRepository.count());
        }
        if (subscribedAgent.getState().equals(Agent.State.IDLING)) {
            logger.debug("agent [{}] is idling at subscribe because didn't find job", subscribedAgent.getName());
        }

        //TODO this event is sent every 10 seconds (jobPollInteval) per agent
        //TODO Check if it is necessary to send it
        //logger.info(">>> Modified Agent " +agent.getHost()+ " subscribe");
        broadcastMessage(MODIFIED_AGENT, subscribedAgent);
        return Response.ok(job).build();
    }

    private Job findJob(Set<String> capabilities, Specification<Job> additionalSpec, String agentGroup) {
        Sort sort = Sort.by(Sort.Direction.ASC, "submitTime")
                .and(Sort.by(Sort.Direction.DESC, "priority"));

        List<Job> jobs;
        Specification<Job> spec;    // WHERE
        // Search Job with requirements

        /*if (!capabilities.isEmpty()) { // if agent has capabilities
            Query<Job> requirementsQuery = basicQuery.cloneQuery();
            jobs = requirementsQuery.field("suite.requirements").in(capabilities).asList();
        }*/
        if (capabilities != null && !capabilities.isEmpty()) {
            spec = Specification.where(JobSpecifications.isReadyOrRunning())
                    .and(JobSpecifications.hasAnyOfCapabilitiesInRequirements(capabilities));    // AND
            if (agentGroup != null) {
                spec = spec.and(JobSpecifications.hasAgentGroup(agentGroup));   // AND
            }
            if (additionalSpec != null) {
                spec = spec.and(additionalSpec);       // AND
            }

            jobs = jobRepository.findAll(spec, sort);   // orderBy: submitTime, DESC
            if (!jobs.isEmpty()) {
                return jobs.get(0); // job with capabilities
            }
        }

        // --- Here Specification gets reset because no jobs were found
        // Search Job without requirements
        spec = Specification.where(JobSpecifications.isReadyOrRunning())    // WHERE
                .and(JobSpecifications.hasNoRequirements());                // AND

        if (agentGroup != null) {
            spec = spec.and(JobSpecifications.hasAgentGroup(agentGroup));   // AND
        }
        if (additionalSpec != null) {
            spec.and(additionalSpec);       // AND
        }

        jobs = jobRepository.findAll(spec, sort);

        return jobs.isEmpty() ? null : jobs.get(0); // job without capabilities
    }

    private Build getLatestBuild(String branch) {
        return buildRepository.findLatestBuildByBranch(branch);
    }

    private int getNotPausedHighestPriorityJob() {
        Optional<PrioritizedJob> prioritizedJob = prioritizedJobRepository.findTopByIsPausedFalseOrderByPriorityDesc(); // highest: 4, lowest: 0
        if (prioritizedJob.isPresent()) {
            return prioritizedJob.map(pj -> {
                logger.info("Highest Priority of jobs: " + pj.getPriority());
                return pj.getPriority();
            }).orElseGet(() -> {
                logger.info("Didn't find prioritized jobs, returning 0");
                return 0;
            });
        }
        logger.info("Didn't find prioritized jobs, returning 0");
        return 0;
    }


    @GET
    @Path("prioritizedJob/{agentId}/{jobId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Boolean hasHigherPriorityJob(@PathParam("agentId") final String agentId, @PathParam("jobId") final String jobId) {

        Optional<Agent> opAgent = agentRepository.findById(agentId);
        Agent agent = opAgent.orElseThrow(() -> new RuntimeException("Agent [" + agentId + "] does not exist"));;

        List<PrioritizedJob> prioritizedJobs = prioritizedJobRepository.findByIsPausedFalseOrderByPriorityDesc();
        if (prioritizedJobs.isEmpty()) {
            return false;
        }

        Optional<Job> opJob = jobRepository.findById(jobId);
        Job job = opJob.orElseThrow(() -> new RuntimeException("Job [" + jobId + "] does not exist"));

        for (PrioritizedJob prioritizedJob : prioritizedJobs) {
            if (prioritizedJob.getPriority() > job.getPriority()) {
                if (prioritizedJob.getAgentGroups().contains(agent.getGroupName()) && agent.getCapabilities().containsAll(prioritizedJob.getRequirements())) {
                    if (jobRepository.count(JobSpecifications.whereJobId(prioritizedJob.getJobId()).and(hasPreparingAgents())) == 1) {
                        return true;
                    }
                }
            } else {
                break;
            }
        }
        return false;
    }

    @POST
    @Path("job/{jobId}/edit")
    @Produces(MediaType.APPLICATION_JSON)
    public Job changeJob(final @PathParam("jobId") String jobId, EditJobRequest jobRequest) {
        Optional<Job> opJob = jobRepository.findById(jobId);
        Job job = opJob.orElseThrow(() -> new RuntimeException("Job [" + jobId + "] does not exist"));;

        int currPriority = job.getPriority();
        Set<String> currAgentGroups = job.getAgentGroups();
        synchronized (changeJobPriorityLock) {
            boolean jobDidNotChange = currPriority == jobRequest.getPriority() && currAgentGroups.equals(jobRequest.getAgentGroups());
            if (job.getState() == State.DONE || jobDidNotChange) {
                return job;
            }

            job.setPriority(jobRequest.getPriority());
            job.setAgentGroups(jobRequest.getAgentGroups());

            job = jobRepository.save(job);
            if (currPriority != jobRequest.getPriority()) {
                if (currPriority == 0) {
                    createPrioritizedJob(job);
                } else {
                    if (jobRequest.getPriority() == 0) {
                        deletePrioritizedJob(job);
                    } else {
                        Optional<PrioritizedJob> opPrioritizedJob = prioritizedJobRepository.findByJobId(job.getId());
                        if (!opPrioritizedJob.isPresent()) {
                            return null;
                        }
                        PrioritizedJob prioritizedJob = opPrioritizedJob.get();
                        prioritizedJob.setPriority(jobRequest.getPriority());
                        prioritizedJob.setAgentGroups(jobRequest.getAgentGroups());

                        highestPriorityJob = getNotPausedHighestPriorityJob();
                    }
                }
            }
        }

        broadcastMessage(MODIFIED_JOB, job);
        return job;
    }

    @GET
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<BuildDTO> getBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("50") @QueryParam("limit") int limit,
                                  @DefaultValue("false") @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

        Sort sort = convertOrderByToSort(orderBy);

        Pageable pageable = all ? Pageable.unpaged() : convertOffsetLimitToPage(offset, limit, sort);
        List<PBuildForView> buildProjections = buildRepository.findAllForViewList(pageable).getContent();
        List<BuildDTO> builds = new ArrayList<>(buildProjections.size());

        for (PBuildForView buildProjection : buildProjections) {
            builds.add(new BuildDTO(buildProjection));
        }
        return new Batch<>(builds, offset, limit, all, orderBy, uriInfo);
    }


    @GET
    @Path("latest-builds")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Build> getLatestBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                        @DefaultValue("50") @QueryParam("limit") int limit,
                                        @DefaultValue("false") @QueryParam("all") boolean all,
                                        @QueryParam("branch") String branchStr,
                                        @QueryParam("tags") String tagsStr,
                                        @DefaultValue("true") @QueryParam("with-all-jobs-completed") boolean withAllJobsCompleted,
                                        @Context UriInfo uriInfo) {

        Specification<Build> spec = Specification.where(null);
        Set<String> tagsSet = null;

        if (tagsStr != null && !tagsStr.isEmpty()) {
            tagsSet = new HashSet<>(Arrays.asList(tagsStr.split("\\s*,\\s*")));
        }

        if (tagsSet != null && !tagsSet.isEmpty()) {
            spec = spec.and(BuildSpecifications.hasAllTags(tagsSet));
        }

        if (branchStr != null && !branchStr.isEmpty()) {
            spec = spec.and(BuildSpecifications.hasBranch(branchStr));
        }

        if (withAllJobsCompleted) {
            spec = spec.and(BuildSpecifications.hasAllJobsCompleted());
        }

        List<String> orderBy = Collections.singletonList("-buildTime");
        Sort sort = convertOrderByToSort(orderBy);

        List<Build> builds;
        if (all) {
            builds = buildRepository.findAll(spec, sort);
        } else {
            Pageable pageable = convertOffsetLimitToPage(offset, limit, sort);
            builds = buildRepository.findAll(spec, pageable).getContent();
        }

        return new Batch<>(builds, offset, limit, all, orderBy, uriInfo);
    }

    @PUT
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build createBuild(Build build) {
        if (build.getBuildTime() == null) {
            build.setBuildTime(new Date());
        }
        build = buildRepository.save(build);
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

        Build found = buildRepository.findByName(build.getName());
        if (found == null) {
            throw new BadRequestException("can't append to build " + build + " since it does not exists");
        }

        if (build.getId() != null && !found.getId().equals(build.getId())) {
            throw new UnsupportedOperationException("appending build id is not supported");
        }

        if (build.getBranch() != null && !build.getBranch().equalsIgnoreCase(found.getBranch())) {
            throw new UnsupportedOperationException("appending branch is not supported");
        }

        if (build.getShas() != null && !build.getShas().isEmpty()) {
            found.getShas().putAll(build.getShas());
        }

        if (build.getResources() != null && !build.getResources().isEmpty()) {
            List<URI> appendedResources = (List<URI>) found.getResources();
            appendedResources.addAll(build.getResources());
            // replace resources with appended
            found.setResources(appendedResources);
        }

        if (build.getTestsMetadata() != null && !build.getTestsMetadata().isEmpty()) {
            List<URI> appendedMetadata = (List<URI>) found.getTestsMetadata();
            appendedMetadata.addAll(build.getTestsMetadata());
            // replace metadata with appended
            found.setTestsMetadata(appendedMetadata);
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
        Optional<Build> optional = buildRepository.findById(id);
        if (!optional.isPresent()) {
            return null;
        }

        Build existingBuild = optional.get();

        if (build.getShas() != null) {
            existingBuild.setShas(build.getShas());
        }
        if (build.getBranch() != null) {
            existingBuild.setBranch(build.getBranch());
        }
        if (build.getResources() != null) {
            existingBuild.setResources(build.getResources());
        }
        if (build.getTestsMetadata() != null) {
            existingBuild.setTestsMetadata(build.getTestsMetadata());
        }
        if (build.getTags() != null) {
            existingBuild.setTags(build.getTags());
        }

        Build savedBuild = buildRepository.save(existingBuild);
        broadcastMessage(MODIFIED_BUILD, savedBuild);

        return savedBuild;
    }

    @POST
    @Path("job/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job updateJob(final @PathParam("id") String id, final Job updatedJob) {
        Optional<Job> opJob = jobRepository.findById(id);
        if (!opJob.isPresent()) {
            return null; // or throw, depending on your logic
        }

        Job job = opJob.get();

        if (updatedJob.getState() != null) {
            job.setState(updatedJob.getState());
        }

        Job savedJob = jobRepository.save(job);
        broadcastMessage(MODIFIED_JOB, savedJob);

        return savedJob;
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

        Optional<Suite> opSuite = suiteRepository.findById(suite.getId());
        Suite existingSuite = opSuite.orElseThrow(() -> new RuntimeException("Suite [" + suite.getId() + "] does not exist"));;

        if (suite.getCriteria() != null) {
            existingSuite.setCriteria(suite.getCriteria());
        }
        if (suite.getCustomVariables() != null) {
            existingSuite.setCustomVariables(suite.getCustomVariables());
        }
        if (suite.getName() != null) {
            existingSuite.setName(suite.getName());
        }
        if (suite.getRequirements() != null) {
            existingSuite.setRequirements(suite.getRequirements());
        }

        Suite savedSuite = suiteRepository.save(existingSuite);
        broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(PSuiteDTO.fromEntity(savedSuite)));

        return suite;
    }


    @GET
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildDTO getBuild(final @PathParam("id") String id) {
        Optional<Build> opBuild = buildRepository.findById(id);
        Build build = opBuild.orElseThrow(() -> new RuntimeException("Build [" + id + "] does not exist"));;

        logger.info("Build [" + build.getName() + "] [" + build.getId() + "] shas:" + Arrays.toString(build.getShas().entrySet().toArray()));
        return BuildDTO.fromEntity(build);
    }

    // TODO - 3 methods are going below might be needed. Review is needed
    /*@GET
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
    }*/

    @DELETE
    @Path("job/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteJob(final @PathParam("jobId") String jobId) {
        Optional<Job> opJob = jobRepository.findById(jobId);
        if (!opJob.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Job deleteCandidate = opJob.get();
        jobRepository.deleteById(deleteCandidate.getId());

        if (deleteCandidate.getPriority() > 0) {
            deletePrioritizedJob(deleteCandidate);
        }
        performDeleteTestsLogs(jobId);
        performDeleteJobSetupLogs(jobId);
        updateBuildWithDeletedJob(deleteCandidate);

        testRepository.deleteByJobId(jobId);

        return Response.ok(Entity.json(jobId)).build();
    }
    @POST
    @Path("jobs/deletejobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized Iterable<Job> deletejobs(final List<String> ids) {
        Iterable<Job> jobs = jobRepository.findAllById(ids);
        for (Job deleteJobCandidate : jobs) {
            jobRepository.deleteById(deleteJobCandidate.getId());
            if (deleteJobCandidate.getPriority() > 0) {
                deletePrioritizedJob(deleteJobCandidate);
            }
            performDeleteTestsLogs(deleteJobCandidate.getId());
            performDeleteJobSetupLogs(deleteJobCandidate.getId());
            updateBuildWithDeletedJob(deleteJobCandidate);
            testRepository.deleteByJobId(deleteJobCandidate.getId());
            broadcastMessage(MODIFIED_JOB, deleteJobCandidate);
        }
        return jobs;
    }

    private void deletePrioritizedJob(Job job) {
        prioritizedJobRepository.deleteByJobId(job.getId());
        if (job.getPriority() == highestPriorityJob) {
            highestPriorityJob = getNotPausedHighestPriorityJob();
        }
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
        Optional<Build> opAssociatedBuild = buildRepository.findById(deletedJob.getBuild().getId());
        if (opAssociatedBuild.isPresent()) {
            Build associatedBuild = opAssociatedBuild.get();
            BuildStatus associatedBuildStatus = associatedBuild.getBuildStatus();
            State state = deletedJob.getState();

            if (state == State.DONE) {
                if (associatedBuildStatus.getDoneJobs() > 0) {
                    associatedBuild.getBuildStatus().decDoneJobs();
                }
            } else if (state == State.PAUSED) {
                if (associatedBuildStatus.getPendingJobs() > 0) {
                    associatedBuild.getBuildStatus().decPendingJobs();
                }
            } else if (state == State.BROKEN) {
                if (associatedBuildStatus.getBrokenJobs() > 0) {
                    associatedBuild.getBuildStatus().decBrokenJobs();
                }
            }

            associatedBuild.getBuildStatus().decTotalJobs();

            Suite suite = deletedJob.getSuite();
            if (suite != null) {
                associatedBuild.getBuildStatus().getSuites()
                        .remove(new BuildStatusSuite(suite.getId(), suite.getName()));
            }

            buildRepository.save(associatedBuild);
            broadcastMessage(MODIFIED_BUILD, associatedBuild);
        }
    }

    @DELETE
    @Path("agent/{agentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAgent(final @PathParam("agentId") String agentId) {
        agentRepository.deleteById(agentId);
        broadcastMessage(MODIFIED_AGENTS_COUNT, agentRepository.count());
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
    @Path("suite/{suiteId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSuite(final @PathParam("suiteId") String suite) {
        Suite suiteToDelete = suiteRepository.findById(suite).orElse(null);
        if (suiteToDelete != null) {
            suiteRepository.deleteById(suiteToDelete.getId());
            broadcastMessage(DELETED_SUITE, suiteToDelete);
            return Response.ok(Entity.json(suiteToDelete)).build();
        } else {
            logger.info("The suite {} doesn't exist", suite);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
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
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDb() throws JsonProcessingException {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("suite/{sourceSuiteId}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSuite(final @PathParam("sourceSuiteId") String sourceSuiteId, final @PathParam("name") String name) {
        Suite sourceSuite = getSuite(sourceSuiteId);

        if (sourceSuite == null) {
            logger.info("The suite {} doesn't exist", sourceSuiteId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (suiteRepository.existsByName(name)) {
            logger.info("Failed to create suite with name " + name);
            return Response.status(Response.Status.BAD_REQUEST).entity("Suite [" + name + "] already exists").build();
        }

        Suite duplicatedSuite = sourceSuite;
        duplicatedSuite.setName(name);
        duplicatedSuite.setId(null);

        suiteRepository.save(duplicatedSuite);
        logger.info("---addSuite---" + duplicatedSuite);
        broadcastMessage(CREATED_SUITE, duplicatedSuite);
        return Response.ok(duplicatedSuite).build();
    }

    @POST
    @Path("suite")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Suite addSuite(Suite suite) {
        suiteRepository.save(suite);
        logger.info("---addSuite---" + suite);
        broadcastMessage(CREATED_SUITE, suite);
        return suite;
    }

    @GET
    @Path("suite/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Suite getSuite(final @PathParam("id") String id) {
        return suiteRepository.findById(id).orElse(null);
    }

    @GET
    @Path("suite")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<SuiteView> getAllSuites(@DefaultValue("0") @QueryParam("offset") int offset,
                                         @DefaultValue("30") @QueryParam("limit") int limit
            , @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

        List<PSuiteThin> suites = getThinSuites(true);
        List<SuiteView> suiteViews = new ArrayList<>(suites.size());
        for (PSuiteThin suite : suites) {
            suiteViews.add(new SuiteView(suite));
        }

        return new Batch<>(suiteViews, offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("availableAgentGroups")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getAvailableAgentGroups() {
        Set<String> availableAgentGroups = agentRepository.findDistinctAgentGroups();
        return availableAgentGroups;
    }

    @POST
    @Path("job-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JobConfig addJobConfig(JobConfig jobConfig) {
        jobConfigRepository.save(jobConfig);
        logger.info("---addJobConfig---" + jobConfig);
        broadcastMessage(CREATED_JOB_CONFIG, jobConfig);
        return jobConfig;
    }

    @GET
    @Path("job-config-by-id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobConfig getJobConfigById(final @PathParam("id") String id) {
        JobConfig jobConfig = jobConfigRepository.findById(id).orElse(null);
        return jobConfig;
    }

    @GET
    @Path("job-config/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobConfig getJobConfigByName(final @PathParam("name") String name) {
        JobConfig jobConfig = jobConfigRepository.findByName(name).orElse(null);
        return jobConfig;
    }

    @GET
    @Path("job-config")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JobConfig> getAllJobConfig() {

        List<JobConfig> jobConfigs = jobConfigRepository.findAll();

        return jobConfigs;
    }

    @POST
    @Path("job-config-from-gui")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JobConfig addJobConfig(@QueryParam("name") String name, @QueryParam("javaVersion") String javaVersion) {
        JobConfig jobConfig = new JobConfig();
        jobConfig.setJavaVersion(JavaVersion.valueOf(javaVersion));
        jobConfig.setName(name);

        jobConfigRepository.save(jobConfig);
        broadcastMessage(CREATED_JOB_CONFIG, jobConfig);
        return jobConfig;
    }

    @GET
    @Path("java-versions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<JavaVersion> getAllJavaVersions() {

        return Arrays.asList(JavaVersion.values());
    }

    /**
     * With only if and name
     *
     * @return
     */
    private List<PSuiteThin> getThinSuites(boolean withCustomVariables) {
        List<PSuiteThin> suites;
        if (withCustomVariables) {
            suites = suiteRepository.findAllThinWithCustomVariablesOrderedByName();
        } else {
            suites = suiteRepository.findAllThinNoCustomVariablesOrderedByName();
        }

        return suites;
    }

    @GET
    @Path("suites-dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSuitesWithJobs() {

        List<PSuiteThin> suites = getThinSuites(false);//suiteDAO.find(suitesQuery).asList();

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

        Optional<PTestForHistory> opThisTest = testRepository.findTestForHistoryById(id);
        PTestForHistory thisTest = opThisTest.orElseThrow(() -> new RuntimeException("Test [" + id + "] does not exist"));

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "--getTests() history, testId=" + id + ",jobId=" + thisTest.getJobId() + ", buildId=" + thisTest.getBuildId() +
                            ", branch=" + thisTest.getBranch() + ", endTime=" + thisTest.getEndTime());
        }

        String sha = thisTest.getBranch().equals(MASTER_BRANCH_NAME) ? thisTest.getSha() : null;
        List<String> arguments = Arrays.asList(thisTest.getArguments().split(" "));
        Specification<Test> spec =
                TestSpecifications.findRecentTestsByNameArgsAndShaAndBranch(thisTest.getName(), arguments, sha, Arrays.asList(MASTER_BRANCH_NAME, thisTest.getBranch()));

        Pageable pageable = convertOffsetLimitToPage(offset, limit, Sort.by(Sort.Direction.DESC, "endTime"));
        List<Test> tests = testRepository.findAll(spec, pageable).getContent();

        if (logger.isDebugEnabled()) {
            logger.debug("--getTests() history, testId=" + id + ", tests size:" + tests.size());
        }

        List<TestHistoryItem> testHistoryItemsList = new ArrayList<>(tests.size());
        if (!tests.isEmpty()) {
            //logger.info("DEBUG (getTests) get test history of testId: [{}], (thisTest: [{}])", id, thisTest);
            List<String> suiteJobsIds = jobRepository.findJobIdsBySuiteId(thisTest.getSuiteId());   //retrieve all jobs that belong to same suite
            for (Test test : tests) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "--getTests() history, test.getEndTime()=" + test.getEndTime() + ", tests size:"
                                    + tests.size());
                }
                //don't bring tests that were ran after this test on any branch
                if (suiteJobsIds.contains(test.getJobId())
                        && (thisTest.getEndTime() == null || (test.getEndTime() != null && test.getEndTime().compareTo(thisTest.getEndTime()) <= 0))) {
                    //logger.info("DEBUG (getTests) ---- > create testHistoryItem to [{}]", test);
                    TestHistoryItem testHistoryItem = createTestHistoryItem(test);
                    if (testHistoryItem == null) {
                        continue;
                    }
                    //logger.info("DEBUG (getTests) ---- > testHistoryItem is: [{}]", testHistoryItem);
                    testHistoryItemsList.add(testHistoryItem);
                }
            }
        }

        return new Batch<>(testHistoryItemsList, offset, limit, false, Collections.emptyList(), uriInfo);
    }

    private TestHistoryItem createTestHistoryItem(Test test) {

        Job job = getJob(test.getJobId());
        if (job == null) {
            return null;
        }
        //logger.info("DEBUG (createTestHistoryItem) get job of test ---- > test: [{}], job: [{}])", test, job);
        return new TestHistoryItem(new TestView(test), new JobView(job));
    }

    private SuiteWithJobs createSuiteWithJobs(PSuiteThin suite) {
        Pageable pageable = PageRequest.of(0, maxJobsPerSuite, Sort.by(Sort.Direction.DESC, "endTime"));

        List<Job> jobsList = jobRepository.findTopJobsForSuiteDoneState(suite.getId(), pageable);

        // addRequiredJobTableColumns(jobsQuery); --> JobProjection

        return new SuiteWithJobs(suite, jobsList);
    }

    private Map<String, List<PJobForDashboard>> createActiveJobsMap(List<Build> builds) {
        Map<String, List<PJobForDashboard>> resultsMap = new HashMap<>();
        for (Build build : builds) {
            resultsMap.put(build.getId(), jobRepository.findByBuildIdAndState(build.getId(), State.RUNNING));
        }

        return resultsMap;
    }

    @POST
    @Path("clearPaused")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearPausedJobs() {
        List<Job> pausedJobs = jobRepository.findByState(State.PAUSED);
        try {
            for (Job j : pausedJobs) {
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
        try {
            // PAUSE jobs that are in RUNNING or READY state
            jobRepository.updateJobsState(State.PAUSED, State.RUNNING, State.READY);

            // repeatedly seek for those are still completing the tests even having PAUSED state
            List<Job> jobList = jobRepository.findByStateAndRunningTestsGreaterThan(State.PAUSED, 0);
            while (!jobList.isEmpty()) {
                logger.info("waiting for all agents to finish running tests, {} jobs are still running:", jobList.size());
                for (Job job : jobList) {
                    logger.info("{},", job.getId());
                }
                Thread.sleep(10000);
                jobList = jobRepository.findByStateAndRunningTestsGreaterThan(State.PAUSED, 0);
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
                    List<FutureJob> futureJobs = futureJobRepository.findAll();
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
        Optional<Agent> opAgent = agentRepository.findById(agent.getId());
        if (!opAgent.isPresent()) {
            return null;
        }
        Agent existingAgent = opAgent.get();
        existingAgent.setSetupRetries(numberOfRetries);
        agentRepository.save(existingAgent);

        broadcastMessage(MODIFIED_AGENT, existingAgent);

        if ((setupRetriesBefore == 0 && numberOfRetries > 0) || (setupRetriesBefore > 0 && numberOfRetries == 0)) {
            long count = agentRepository.countBySetupRetriesGreaterThan(0);
            broadcastMessage(MODIFIED_FAILING_AGENTS, count);
        }

        return existingAgent;
    }

    private Suite createSuiteFromFailingTests(String jobId, String newSuiteName) throws Exception {
        if (jobId == null || newSuiteName == null || newSuiteName.length() == 0) {
            throw new Exception("Required parameters are not valid");

        }
        Optional<Job> opJob = jobRepository.findById(jobId);

        Job existingJob = opJob.orElseThrow(() -> new RuntimeException("Job [" + jobId + "] does not exist"));

        if (suiteRepository.existsByName(newSuiteName)) {
            throw new Exception("Suite [" + newSuiteName + "] already exists");
        }

        List<Test> failedTests = testRepository.findByJobIdAndStatusAndRunNumber(jobId, Test.Status.FAIL, 1);
        if (failedTests.size() == 0) {
            throw new Exception("Job [" + jobId + "] has no failed tests");
        }

        Suite suite = existingJob.getSuite();
        String testType = getTestType(suite.getCriteria());

        if (testType == null) {
            throw new Exception("Could not analyze testType from suite");
        }
        else if(suite.getCriteria() instanceof SuiteCriteria)
        {
            List<Criteria> include = new ArrayList<>();
            List<Criteria> exclude = Collections.emptyList();
            failedTests.forEach(list->include.add(TestCriteria.createCriteriaByTestArgs(list.getArguments())));

            suite.setCriteria(new SuiteCriteria(include,exclude,testType));
            suite.setId(null);
            suite.setName(newSuiteName);
        }
        else {
            List<Criteria> criteriaList = new LinkedList<>();
            failedTests.forEach(list->criteriaList.add(TestCriteria.createCriteriaByTestArgs(list.getArguments())));
            suite.setId(null);
            suite.setName(newSuiteName);

            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.include(criteriaList));
            suite.setCriteria(criteria);
        }


        return suite;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("suite/failedTests")
    public Response restCreateSuiteFromFailingTests(@QueryParam("jobId") String jobId, @QueryParam("suiteName") String newSuiteName) {
        try {
            Suite suite = createSuiteFromFailingTests(jobId, newSuiteName);

            addSuite(suite);

            logger.info("Created suite: " + suite);
            return Response.ok(suite).build();
        } catch (Exception e) {
            logger.info("Failed to create suite ", e);

            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }


    private String getTestType(Criteria criteria) {
        if (criteria instanceof TestCriteria) {
            TestCriteria tmp = (TestCriteria) criteria;
            return tmp.getTest().getTestType();
        }
        if (criteria instanceof SuiteCriteria) {
            return ((SuiteCriteria) criteria).getSuiteType();
        }
        if (criteria instanceof AndCriteria) {
            AndCriteria tmp = (AndCriteria) criteria;
            for (Criteria c : tmp.getCriterias()) {
                String res = getTestType(c);
                if (res != null) {
                    return res;
                }
            }
        }
        if (criteria instanceof OrCriteria) {
            OrCriteria orCriteria = (OrCriteria) criteria;
            for (Criteria c : orCriteria.getCriterias()) {
                String res = getTestType(c);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }
    // events

    private void broadcastMessage(String type, Object value) {
        if (value != null) {
            value = prepareEntityForBroadcasting(value);
            try {
                long time1 = System.currentTimeMillis();
                EventSocket.broadcast(new Message(type, value));
                long time2 = System.currentTimeMillis();
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Broadcasting message [" + type + "] with value [" + value + "] took " + (
                                    time2 - time1) + " ms");
                }
            } catch (Throwable e) {
                logger.error("Invoking of broadcastMessage() failed: ", e);
            }
        }
    }

    private Object prepareEntityForBroadcasting(Object value) {
        Object entity = value;
        if (value instanceof Job) {     // Job
            Job job = (Job) value;
            if (job.getBuild().getBuildStatus() != null) {
                job.getBuild().getBuildStatus().setBuild(null);
            }
            if (job.getJobSetupLog() != null) {
                job.getJobSetupLog().setJob(null);
            }
        } else if (value instanceof Test) {     // Test
            Test test = (Test) value;
            if (test.getLogs() != null) {
                test.getLogs().setTest(null);
            }
        }  else if (value instanceof Build) {       // Build
            Build build = (Build) value;
            if (build.getBuildStatus() != null) {
                build.getBuildStatus().setBuild(null);
            }
        }

        return entity;
    }

    private List<Agent> getAgentsNotSeenInLastMillis(long delay) {
        Date timeThreshold = new Date(System.currentTimeMillis() - delay);
        return agentRepository.findAgentsNotSeenInLastMillis(Agent.State.IDLING, timeThreshold);
    }

    private List<Agent> getZombieAgents(long delay) {
        Date timeThreshold = new Date(System.currentTimeMillis() - delay);
        return agentRepository.findZombieAgents(Agent.State.IDLING, timeThreshold);
    }

    private void handleZombieAgent(Agent agentToDelete) {
        logger.warn("Agent {} did not report on time while he was IDLING and will be deleted", agentToDelete.getName());

        String agentIp = agentToDelete.getHostAddress();
        offlineAgents.put(agentIp, new OfflineAgent(agentIp, agentToDelete.getHost(), agentToDelete.getHostAddress(), agentToDelete.getLastTouchTime()));
        agentRepository.deleteById(agentToDelete.getId());

        broadcastMessage(DELETED_AGENT, agentToDelete);
        broadcastMessage(CREATED_OFFLINE_AGENT, createAgentFromOfflineAgent(offlineAgents.get(agentIp)));
        broadcastMessage(MODIFIED_AGENTS_COUNT, agentRepository.count());
        //Delete agent from preparing agents in jobs
        if (StringUtils.notEmpty(agentToDelete.getJobId())) {
            Optional<Job> opJob = jobRepository.findById(agentToDelete.getJobId());
            if (opJob.isPresent()) {
                Job job = opJob.get();
                job.getPreparingAgents().remove(agentToDelete.getName());
                jobRepository.save(job);
            }
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
        Set<Test> runningTestsPerAgent = new HashSet<>();
        // reset tests
        for (String testId : agent.getCurrentTests()) {
            Optional<Test> opTest = testRepository.findByIdAndStatusAndAssignedAgent(testId, Test.Status.RUNNING, agent.getName());
            // reset all tests were previously run by the current agent to their initial state
            if (opTest.isPresent()) {
                Test existingTest = opTest.get();
                existingTest.setAssignedAgent(null);
                existingTest.setAgentGroup(null);
                existingTest.setStartTime(null);
                existingTest.setStatus(Test.Status.PENDING);

                existingTest = testRepository.save(existingTest);
                logger.warn("test {} was released since agent {} not seen for a long time", existingTest.getId(), agent.getName());
                runningTestsPerAgent.add(existingTest);
            }
        }
        Job job = null;
        // reset job
        if (agent.getJobId() != null) {
            Optional<Job> opJob = jobRepository.findById(agent.getJobId());
            if (agent.getState() == Agent.State.PREPARING) {
                if (opJob.isPresent()) {
                    job = opJob.get();
                    job.getPreparingAgents().removeAll(Collections.singletonList(agent.getName()));

                    job = jobRepository.save(job);
                }
            } else if (agent.getState() == Agent.State.RUNNING && !runningTestsPerAgent.isEmpty()) {
                logger.info("returnTests for agent [{}], jobId [{}], amount of running tests by the agent [{}]", agent.getName(), agent.getJobId(), runningTestsPerAgent);
                if (opJob.isPresent()) {
                    job = opJob.get();
                    job.setRunningTests(job.getRunningTests() - runningTestsPerAgent.size());

                    job = jobRepository.save(job);
                }
            }
        }
        Optional<Agent> opAgent = agentRepository.findById(agent.getId());
        // reset agent
        Agent existingAgent = opAgent.orElseThrow(() -> new RuntimeException("Agent [" + agent.getId() + "] does not exist"));;
        existingAgent.setCurrentTests(new HashSet<>());
        existingAgent.setState(Agent.State.IDLING);

        existingAgent = agentRepository.save(existingAgent);

        if (existingAgent.getState().equals(Agent.State.IDLING)) {
            logger.warn("agent [{}] is idling while returning tests to pool", existingAgent.getName());
        }
        for (Test test : runningTestsPerAgent) {
            broadcastMessage(MODIFIED_TEST, test);
        }
        if (job != null) {
            broadcastMessage(MODIFIED_JOB, job);
        }

        existingAgent.setJob(job);  // add job for the broadcasting
        broadcastMessage(MODIFIED_AGENT, existingAgent);
    }

    private void checkServerStatus() {
        synchronized (this.serverStatusLock) {
            if (this.serverStatus.getStatus().equals(ServerStatus.Status.SUSPENDED)) {
                throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN_TYPE).entity("Server is suspended, please try again later.").build());
            }
        }
    }
}
