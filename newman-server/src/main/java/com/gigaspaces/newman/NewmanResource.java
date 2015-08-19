package com.gigaspaces.newman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.config.Config;
import com.gigaspaces.newman.dao.*;
import com.gigaspaces.newman.utils.FileUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.util.JSON;
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
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
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

    private final SseBroadcaster broadcaster;
    private final MongoClient mongoClient;
    private final JobDAO jobDAO;
    private final TestDAO testDAO;
    private final BuildDAO buildDAO;
    private final AgentDAO agentDAO;
    private final SuiteDAO suiteDAO;
    private final Config config;
    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "tests-logs";
    @SuppressWarnings("FieldCanBeLocal")
    private final Timer timer = new Timer(true);

    private Morphia morphia;

    private final ConcurrentHashMap<String, Object> agentLocks = new ConcurrentHashMap<>();

    private static final int maxJobsPerSuite = 5;
    private final DistinctIterable distinctTestsByAssignedAgentFilter;

    private final static String CRITERIA_PROP_NAME = "criteria";

    public NewmanResource(@Context ServletContext servletContext) {
        this.config = Config.fromString(servletContext.getInitParameter("config"));
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
        morphia = new Morphia().mapPackage("com.gigaspaces.newman.beans.criteria").mapPackage("com.gigaspaces.newman.beans");
        Datastore ds = morphia.createDatastore(mongoClient, config.getMongo().getDb());
        ds.ensureIndexes();
        ds.ensureCaps();
        jobDAO = new JobDAO(morphia, mongoClient, config.getMongo().getDb());
        testDAO = new TestDAO(morphia, mongoClient, config.getMongo().getDb());
        buildDAO = new BuildDAO(morphia, mongoClient, config.getMongo().getDb());
        agentDAO = new AgentDAO(morphia, mongoClient, config.getMongo().getDb());
        suiteDAO = new SuiteDAO(morphia, mongoClient, config.getMongo().getDb());

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
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Job> jobs(@DefaultValue("0") @QueryParam("offset") int offset,
                           @DefaultValue("30") @QueryParam("limit") int limit
            , @QueryParam("buildId") String buildId
            , @QueryParam("all") boolean all
            , @QueryParam("orderBy") List<String> orderBy
            , @Context UriInfo uriInfo) {

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

        List<Job> jobs = jobDAO.find(query).asList();
        return new Batch<>(jobs, offset, limit, all, orderBy, uriInfo);
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
            Set<String> agents = (Set) assignedAgents.into(new HashSet<>());
            job.setAgents(agents);
            long filterTestsEnd = System.currentTimeMillis();

            logger.info("distinct filter by job id took {} msec.", (filterTestsEnd - filterTestsStart));
        }

        return job;
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
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Test addTest(Test test) {
        if (test.getJobId() == null) {
            throw new BadRequestException("can't add test with no jobId: " + test);
        }
        UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations().inc("totalTests");
        Job job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(test.getJobId()), jobUpdateOps, false, false);
        if (job != null) {
            test.setStatus(Test.Status.PENDING);
            test.setScheduledAt(new Date());
            testDAO.save(test);
            Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()), buildDAO.createUpdateOperations().inc("buildStatus.totalTests"));
            broadcastMessage(MODIFIED_BUILD, build);
            broadcastMessage(CREATED_TEST, test);
            broadcastMessage(MODIFIED_JOB, job);
//            broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs( job.getSuite() ) );
            return test;
        } else {
            throw new BadRequestException("Can't add test, job does not exists: " + test);
        }
    }


    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public synchronized Test finishTest(final Test test) {
        if (test.getId() == null) {
            throw new BadRequestException("can't finish test without testId: " + test);
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

        if (test.getErrorMessage() != null) {
            testUpdateOps.set("errorMessage", test.getErrorMessage());
        }
        if (status == Test.Status.FAIL || status == Test.Status.SUCCESS) {
            testUpdateOps.set("endTime", new Date());
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
        Build build = buildDAO.getDatastore().findAndModify(buildDAO.createIdQuery(job.getBuild().getId()), updateBuild);

        if (result.getAssignedAgent() != null) {
            UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date())
                    .removeAll("currentTests", test.getId());
            Agent agent = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(result.getAssignedAgent()), updateOps, false, false);
            Agent idling = null;
            if (agent.getCurrentTests().isEmpty()) {
                idling = agentDAO.getDatastore().findAndModify(agentDAO.createQuery().field("name").equal(result.getAssignedAgent())
                                .where("this.currentTests.length == 0"),
                        agentDAO.createUpdateOperations().set("state", Agent.State.IDLING));
            }
            broadcastMessage(MODIFIED_AGENT, idling == null ? agent : idling);
        }

        broadcastMessage(MODIFIED_BUILD, build);
        broadcastMessage(MODIFIED_TEST, result);
        broadcastMessage(MODIFIED_JOB, job);
        broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs(job.getSuite()));
        return result;
    }


    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Test> getJobTests(@DefaultValue("0") @QueryParam("offset") int offset,
                                   @DefaultValue("30") @QueryParam("limit") int limit,
                                   @DefaultValue("false") @QueryParam("all") boolean all,
                                   @QueryParam("orderBy") List<String> orderBy,
                                   @QueryParam("jobId") String jobId, @Context UriInfo uriInfo) {
        Query<Test> query = testDAO.createQuery();
        if (jobId != null) {
            query.field("jobId").equal(jobId);
        }
        if (orderBy != null) {
            orderBy.forEach(query::order);
        }
        if (!all) {
            query.offset(offset).limit(limit);
        }
        return new Batch<>(testDAO.find(query).asList(), offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("test/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("id") String id) {
        return testDAO.findOne(testDAO.createQuery().field("_id").equal(new ObjectId(id)));
    }

    @POST
    @Path("test/{jobId}/{id}/log")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Test uploadLog(FormDataMultiPart form,
                          @PathParam("jobId") String jobId,
                          @PathParam("id") String id,
                          @Context UriInfo uriInfo) {
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
            logger.error("Failed to save log at {} for test {} jobid {}", filePath, testId, jobId, e);
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

    /*
    @POST
    @Path("agent")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Test getTestToRun(Agent agent) {
        logger.info("---getTestToRun() agent=" + agent);
        agent.setLastTouchTime(new Date());
        Agent foundAgent = agentDAO.findOne(agentDAO.createQuery().field("name").equal(agent.getName()));
        if (foundAgent == null) {
            agentDAO.save(agent);
            foundAgent = agent;
        }
        //first try to find test matching the current agent job.
        //if not foundAgent search for any test.
        Test result = getMatchingTest(foundAgent);
        if (result == null && foundAgent.getJobId() != null) {
            foundAgent.setJobId(null);
            result = getMatchingTest(foundAgent);
        }
        if (result == null) {
            return null;
        }

        // test foundAgent.
        String jobId = result.getJobId();

        //update job  state.
        UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().inc("runningTests");
        Query<Job> query = jobDAO.createIdQuery(jobId);
        query.or(query.criteria("state").equal(State.READY), query.criteria("state").equal(State.RUNNING));
        Job job = jobDAO.getDatastore().findAndModify(query, updateJobStatus);
        if (job.getStartTime() == null) {
            updateJobStatus = jobDAO.createUpdateOperations().set("startTime", new Date()).set("state", State.RUNNING);
            job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId).field("state").notEqual(State.PAUSED), updateJobStatus);
            if (job == null) {
                // job was paused after runningTests was inc.
                jobDAO.updateFirst(jobDAO.createIdQuery(jobId), jobDAO.createUpdateOperations().dec("runningTests"));
                return null;
            }
        }

        // update agent state.
        UpdateOperations<Agent> agentUpdateOps = agentDAO.createUpdateOperations().set("lastTouchTime", new Date());
        agentUpdateOps.add("currentTests", result.getId());
        foundAgent = agentDAO.getDatastore().findAndModify(agentDAO.createIdQuery(foundAgent.getId()), agentUpdateOps, false, true);

        //send event to clients.
        broadcastMessage(MODIFIED_TEST, result);
        broadcastMessage(MODIFIED_JOB, job);
//        broadcastMessage(MODIFIED_SUITE, createSuiteWithJobs( job.getSuite() ) );
        broadcastMessage(MODIFIED_AGENT, foundAgent);
        return result;
    }

    private Test getMatchingTest(Agent agent) {
        Query<Test> query = testDAO.createQuery();
        logger.info("---getMatchingTest() agent=" + agent.getJobId());
        if (agent.getJobId() != null) {
            query.criteria("jobId").equal(agent.getJobId());
        } else {
            query.order("scheduledAt");
        }
        query.criteria("status").equal(Test.Status.PENDING);
        UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("status", Test.Status.RUNNING)
                .set("assignedAgent", agent.getName()).set("startTime", new Date());
        return testDAO.getDatastore().findAndModify(query, updateOps, false, false);
    }
*/

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

        Query<Job> query = jobDAO.createQuery();
        query.or(query.criteria("state").equal(State.READY), query.criteria("state").equal(State.RUNNING));
        query.where("this.totalTests != (this.passedTests + this.failedTests + this.runningTests)");
        query.order("-submitTime");
        Job job = jobDAO.findOne(query);
        UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations()
                .set("lastTouchTime", new Date());
        if (agent.getHost() != null) {
            updateOps.set("host", agent.getHost());
            updateOps.set("hostAddress", agent.getHostAddress());
            updateOps.set("pid", agent.getPid());
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


    @GET
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<Build> getBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("30") @QueryParam("limit") int limit,
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
    @Path("suite/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Suite updateSuite(final @PathParam("id") String id, final String suiteStr) {

        logger.info("---updateSuite()");

        DBObject parsedSuite = (DBObject) JSON.parse(suiteStr);

        //**have to perform following changes with received json in order to make it compliant to morphia json mapper**//
        parsedSuite.removeField("id");
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("className", Suite.class.getName());
        linkedHashMap.put("_id", id);
        linkedHashMap.putAll(parsedSuite.toMap());
        //****//

        BasicDBObject basicDBObject = new BasicDBObject(linkedHashMap);
        Object criteriaVal = basicDBObject.get(CRITERIA_PROP_NAME);
        if (criteriaVal != null && criteriaVal.toString().length() > 0) {
            DBObject criteriaDBObject = (DBObject) JSON.parse(criteriaVal.toString());
            basicDBObject.put(CRITERIA_PROP_NAME, criteriaDBObject);
            System.out.println(">>>criteriaDBObject=" + criteriaDBObject);
        }

        Suite suite = morphia.fromDBObject(Suite.class, basicDBObject);

        UpdateOperations<Suite> updateOps = suiteDAO.createUpdateOperations();
        if (suite.getCriteria() != null) {
            updateOps.set(CRITERIA_PROP_NAME, suite.getCriteria());
        }
        if (suite.getCustomVariables() != null) {
            updateOps.set("customVariables", suite.getCustomVariables());
        }
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
        List<String> deleted = new ArrayList<String>();
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

    private Job performDeleteJob(String jobId) {
        Query<Job> idJobQuery = jobDAO.createIdQuery(jobId);
        Datastore datastore = jobDAO.getDatastore();
        Job deletedJob = datastore.findAndDelete(idJobQuery);
        return deletedJob;
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
        Suite suite = suiteDAO.findOne(suiteDAO.createIdQuery(id));
        String displayedCriteriaJson = "";
        if (suite.getCriteria() != null) {
            DBObject dbObject = morphia.toDBObject(suite.getCriteria());
            ObjectMapper mapper = new ObjectMapper();
            try {
                displayedCriteriaJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dbObject);
            } catch (JsonProcessingException e) {
                logger.error(e.toString(), e);
                displayedCriteriaJson = dbObject.toString();
            }
        }

        suite.setDisplayedCriteria(displayedCriteriaJson);

        return suite;
    }

    @GET
    @Path("suite")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Suite> getAllSuites(@DefaultValue("0") @QueryParam("offset") int offset,
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

        return new Batch<>(suiteDAO.find(query).asList(), offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("suites-dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSuitesWithJobs() {
        logger.info("--- getAllSuitesWithJobs() START ---");
        Query<Suite> suitesQuery = suiteDAO.createQuery();

        List<Suite> suites = suiteDAO.find(suitesQuery).asList();
        logger.info("--- getAllSuitesWithJobs(), suites count ---" + suites.size());
        List<SuiteWithJobs> suitesWithJobs = new ArrayList<>(suites.size());
        for (Suite suite : suites) {

            suitesWithJobs.add(createSuiteWithJobs(suite));
        }

        //reverse map in order to display first latest suites
        Collections.reverse(suitesWithJobs);
        logger.info("--- getAllSuitesWithJobs() END ---");

        return Response.ok(Entity.json(suitesWithJobs)).build();
    }

    @GET
    @Path("test-history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTests(@QueryParam("id") String id) {

        Test thisTest = getTest(id);
        String testName = thisTest.getName();
        List<String> testArguments = thisTest.getArguments();

        Query<Test> testsQuery = testDAO.createQuery().field("name").equal(testName).field("arguments").equal(testArguments);
        List<Test> tests = testDAO.find(testsQuery).asList();
        Collections.reverse(tests);

        List<TestHistoryItem> testHistoryItemsList = new ArrayList<>(tests.size());
        for (Test test : tests) {
            TestHistoryItem testHistoryItem = createTestHistoryItem(test);
            testHistoryItemsList.add(testHistoryItem);
        }
        return Response.ok(Entity.json(testHistoryItemsList)).build();
    }


    private TestHistoryItem createTestHistoryItem(Test test) {

        Job job = getJob(test.getJobId());
        return new TestHistoryItem(test, job);
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

    private List<BuildWithJobs> createBuildsWithJobs(List<Build> builds) {
        List<BuildWithJobs> resultList = new ArrayList<>();
        for (Build build : builds) {
            BuildWithJobs buildWithJobs = createBuildWithJobs(build);
            resultList.add(buildWithJobs);
        }

        return resultList;
    }

    private Map<String, List<Job>> createActiveJobsMap(List<Build> builds) {
        Map<String, List<Job>> resultsMap = new HashMap<>();
        for (Build build : builds) {
            resultsMap.put(build.getId(), getActiveBuildJobs(build));
        }

        return resultsMap;
    }

    private BuildWithJobs createBuildWithJobs(Build build) {

        List<Job> jobs = getActiveBuildJobs(build);
        return new BuildWithJobs(build, jobs);
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
        try {
            OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            OutboundEvent event = eventBuilder.name(type)
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .data(value.getClass(), value)
                    .build();

            broadcaster.broadcast(event);
        } catch (Throwable ignored) {
            logger.error("Invoking of broadcastMessage() failed due the [" + ignored.toString() + "], type=" + type + ", value:" + value, ignored);
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
     * Return this agent job and test to the pool, update agent data.
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
            } else if (agent.getState() == Agent.State.RUNNING && !tests.isEmpty()) {
                jobUpdateOps.inc("runningTests", 0 - tests.size());
                job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(agent.getJobId()), jobUpdateOps);
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
