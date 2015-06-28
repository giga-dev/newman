package com.gigaspaces.newman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.BuildStatus;
import com.gigaspaces.newman.beans.BuildWithJobs;
import com.gigaspaces.newman.beans.DashboardData;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.JobRequest;
import com.gigaspaces.newman.beans.State;
import com.gigaspaces.newman.beans.Suite;
import com.gigaspaces.newman.beans.SuiteWithJobs;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.UserPrefs;
import com.gigaspaces.newman.config.Config;
import com.gigaspaces.newman.dao.AgentDAO;
import com.gigaspaces.newman.dao.BuildDAO;
import com.gigaspaces.newman.dao.JobDAO;
import com.gigaspaces.newman.dao.SuiteDAO;
import com.gigaspaces.newman.dao.TestDAO;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "web/logs";
    @SuppressWarnings("FieldCanBeLocal")
    private final Timer timer = new Timer(true);

    private final ConcurrentHashMap<String, Object> agentLocks = new ConcurrentHashMap<>();

    private static final int maxJobsPerSuite = 5;

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
        Morphia morphia = new Morphia().mapPackage("com.gigaspaces.newman.beans.criteria").mapPackage("com.gigaspaces.newman.beans");
        Datastore ds = morphia.createDatastore(mongoClient, config.getMongo().getDb());
        ds.ensureIndexes();
        ds.ensureCaps();
        jobDAO = new JobDAO(morphia, mongoClient, config.getMongo().getDb());
        testDAO = new TestDAO(morphia, mongoClient, config.getMongo().getDb());
        buildDAO = new BuildDAO(morphia, mongoClient, config.getMongo().getDb());
        agentDAO = new AgentDAO(morphia, mongoClient, config.getMongo().getDb());
        suiteDAO = new SuiteDAO(morphia, mongoClient, config.getMongo().getDb());
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getAgentsNotSeenInLastMillis(1000 * 60 * 3).forEach(NewmanResource.this::handleUnseenAgent);
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
        return new Batch<>(jobDAO.find(query).asList(), offset, limit, all, orderBy, uriInfo);
    }

    @GET
    @Path("job/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Job getJob(@PathParam("id") final String id) {
        return jobDAO.findOne(jobDAO.createQuery().field("_id").equal(new ObjectId(id)));
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
                if( state == State.READY ){
                    Query<Test> query = testDAO.createQuery();
                    query.and(query.criteria("jobId").equal(id), query.criteria("status").equal(Test.Status.RUNNING));
                    UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("status", Test.Status.PENDING);

                    UpdateResults update = testDAO.getDatastore().update(query, updateOps);
                    logger.info( "---ToggleJobPause, state is READY, affected count:" + update.getUpdatedCount() );
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
        List<Build> activeBuilds = buildDAO.find(buildDAO.createQuery().where("this.buildStatus.totalJobs>0").where("this.buildStatus.doneJobs < this.buildStatus.totalJobs").order("-buildTime")).asList();
        List<Build> historyBuilds = buildDAO.find(buildDAO.createQuery().where("this.buildStatus.totalJobs>0").where("this.buildStatus.doneJobs == this.buildStatus.totalJobs").limit(5).order("-buildTime")).asList();
        return new DashboardData(activeBuilds, historyBuilds);
    }

    @GET
    @Path("build/history")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<BuildWithJobs> getHistoryJobGroup(@Context UriInfo uriInfo) {
        Query<Job> query = jobDAO.createQuery();
        query.criteria("state").equal(State.DONE);
        query.field("build.id").hasNoneOf(Arrays.asList("foo", "bar"));
        query.order("submitTime");
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
        if (jobId == null) {
            throw new BadRequestException("can't unsubscribe agent without a job " + agent);
        }
        Job job = jobDAO.findOne(jobDAO.createIdQuery(jobId));
        UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().set("state", State.READY);
        jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(job.getId()), updateJobStatus);
        UpdateOperations<Agent> updateAgentOps = agentDAO.createUpdateOperations().unset("jobId");
        agentDAO.getDatastore().updateFirst(agentDAO.createQuery().field("name").equal(agent.getName()), updateAgentOps, true);
        return job;
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
    public Test finishTest(final Test test) {
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
    @Path("test/{id}/log")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Test uploadLog(FormDataMultiPart form,
                          @PathParam("id") String id,
                          @Context UriInfo uriInfo) {
        FormDataBodyPart filePart = form.getField("file");
        ContentDisposition contentDispositionHeader = filePart.getContentDisposition();
        InputStream fileInputStream = filePart.getValueAs(InputStream.class);
        String fileName = contentDispositionHeader.getFileName();
        String filePath = SERVER_UPLOAD_LOCATION_FOLDER + "/" + id + "/" + fileName;
        try {
            saveFile(fileInputStream, filePath);
            URI uri = uriInfo.getAbsolutePathBuilder().path(fileName).build();
            String name = getLogName(fileName);
            UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("logs." + name, uri.toASCIIString());
            Test test = testDAO.getDatastore().findAndModify(testDAO.createIdQuery(id), updateOps);
            broadcastMessage(MODIFIED_TEST, test);
        } catch (IOException e) {
            logger.error("Failed to save log at {} for test {}", filePath, id, e);
        }
        return null;
    }

    @GET
    @Path("test/{id}/log/{name}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response downloadLog(@PathParam("id") String id, @PathParam("name") String name,
                                @DefaultValue("false") @QueryParam("download") boolean download) {
        MediaType mediaType;
        if (download) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }
        String filePath = SERVER_UPLOAD_LOCATION_FOLDER + "/" + id + "/" + name;
        return Response.ok(new File(filePath), mediaType).build();
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
                pj = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(jobId), jobDAO.createUpdateOperations().dec("preparingAgents"));
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
                    UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().dec("preparingAgents");
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
        query.order("submitTime");
        Job job = jobDAO.findOne(query);
        UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations()
                .set("lastTouchTime", new Date());
        if (agent.getHost() != null) {
            updateOps.set("host", agent.getHost());
        }
        updateOps.set("currentTests", new HashSet<String>());
        if (job != null) {
            updateOps.set("jobId", job.getId());
            updateOps.set("state", Agent.State.PREPARING);
            UpdateOperations<Job> updateJobStatus = jobDAO.createUpdateOperations().inc("preparingAgents");
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

    @GET
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build getBuild(final @PathParam("id") String id) {
        return buildDAO.findOne(buildDAO.createIdQuery(id));
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

    private SuiteWithJobs createSuiteWithJobs(Suite suite) {

        String suiteId = suite.getId();
        Query<Job> jobsQuery = jobDAO.createQuery();
        jobsQuery.field("suite.id").equal(suiteId);
        jobsQuery.criteria("state").equal(State.DONE);
        jobsQuery.order("endTime").limit(maxJobsPerSuite);

        List<Job> jobsList = jobDAO.find(jobsQuery).asList();
/*
        logger.info("--- getAllSuitesWithJobs(), jobs list size:" + jobsList.size());
        logger.info("--- getAllSuitesWithJobs(), jobs:" + Arrays.toString(jobsList.toArray(new Job[jobsList.size()])));
*/

        return new SuiteWithJobs(suite, jobsList);
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
        if (agent.getJobId() != null && !tests.isEmpty()) {
            UpdateOperations<Job> jobUpdateOps = jobDAO.createUpdateOperations();
            if (agent.getState() == Agent.State.PREPARING) {
                jobUpdateOps.dec("preparingAgents");
            } else if (agent.getState() == Agent.State.RUNNING) {
                jobUpdateOps.inc("runningTests", 0 - tests.size());
            }
            job = jobDAO.getDatastore().findAndModify(jobDAO.createIdQuery(agent.getJobId()), jobUpdateOps);
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
