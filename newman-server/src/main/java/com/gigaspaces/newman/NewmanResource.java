package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.config.Config;
import com.gigaspaces.newman.dao.AgentDAO;
import com.gigaspaces.newman.dao.BuildDAO;
import com.gigaspaces.newman.dao.JobDAO;
import com.gigaspaces.newman.dao.TestDAO;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Date;
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

    private final MongoClient mongoClient;
    private final Morphia morphia;
    private final Datastore ds;
    private final JobDAO jobDAO;
    private final TestDAO testDAO;
    private final BuildDAO buildDAO;
    private final AgentDAO agentDAO;
    private final Config config;

    public NewmanResource(@Context ServletContext servletContext) {
        this.config = Config.fromString(servletContext.getInitParameter("config"));
        mongoClient = new MongoClient(config.getMongo().getHost());
        morphia = new Morphia().map(Job.class, Agent.class, Build.class, Test.class);
        ds = morphia.createDatastore(mongoClient, config.getMongo().getDb());
        ds.ensureIndexes();
        ds.ensureCaps();
        jobDAO = new JobDAO(morphia, mongoClient, config.getMongo().getDb());
        testDAO = new TestDAO(morphia, mongoClient, config.getMongo().getDb());
        buildDAO = new BuildDAO(morphia, mongoClient, config.getMongo().getDb());
        agentDAO = new AgentDAO(morphia, mongoClient, config.getMongo().getDb());
    }

    @GET
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<Job> jobs(@DefaultValue("0") @QueryParam("offset") int offset,
                           @DefaultValue("30") @QueryParam("limit") int limit, @Context UriInfo uriInfo) {
        return new Batch<>(jobDAO.find(jobDAO.createQuery().offset(offset).limit(limit)).asList(), offset, limit,
                false, uriInfo);
    }

    @PUT
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job createJob(JobRequest jobRequest, @Context SecurityContext sc) {
        Build build = buildDAO.findOne(buildDAO.createQuery().field("_id").equal(new ObjectId(jobRequest.getBuildId())));
        if (build != null) {
            Job job = new Job();
            job.setBuild(build);
            job.setState(State.READY);
            job.setSubmitTime(new Date());
            job.setSubmittedBy(sc.getUserPrincipal().getName());
            job.setResources(jobRequest.getResources());
            jobDAO.save(job);
            return job;
        } else {
            return null;
        }
    }


    @PUT
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Test addTest(Test test) {
        if (test.getJobId() == null) {
            throw new BadRequestException("can't add test with no jobId: " + test);
        }
        if (jobDAO.exists(jobDAO.createQuery().field("_id").equal(new ObjectId(test.getJobId())))) {
            test.setStatus(Test.Status.PENDING);
            test.setScheduledAt(new Date());
            testDAO.save(test);
            return test;
        } else {
            throw new BadRequestException("Can't add test, job does not exists: " + test);
        }
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Test> getJobTests(@DefaultValue("0") @QueryParam("offset") int offset,
                                   @DefaultValue("30") @QueryParam("limit") int limit,
                                   @DefaultValue("false") @QueryParam("all") boolean all,
                                   @QueryParam("jobId") String jobId, @Context UriInfo uriInfo) {
        Query<Test> query = testDAO.createQuery();
        if (jobId != null) {
            query.field("jobId").equal(jobId);
        }
        if (!all) {
            query.offset(offset).limit(limit);
        }
        return new Batch<>(testDAO.find(query).asList(), offset, limit, all, uriInfo);
    }

    @GET
    @Path("test/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("id") String id) {
        return testDAO.findOne(testDAO.createQuery().field("_id").equal(new ObjectId(id)));
    }


    @POST
    @Path("subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job subscribe(final Agent agent) {
        Query<Job> query = jobDAO.createQuery();
        query.or(query.criteria("state").equal(State.READY), query.criteria("state").equal(State.RUNNING));
        Job job = jobDAO.findOne(query);
        UpdateOperations<Agent> updateOps = agentDAO.createUpdateOperations()
                .set("lastTouchTime", new Date());
        if (agent.getHost() != null) {
            updateOps.set("host", agent.getHost());
        }
        if (job != null) {
            updateOps.set("jobId", job.getId());
        }
        agentDAO.getDatastore().updateFirst(agentDAO.createQuery().field("name").equal(agent.getName()), updateOps, true);
        return job;
    }

    @GET
    @Path("subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Agent> getSubscriptions(@DefaultValue("0") @QueryParam("offset") int offset,
                                         @DefaultValue("30") @QueryParam("limit") int limit, @Context UriInfo uriInfo) {
        return new Batch<>(agentDAO.find(agentDAO.createQuery().offset(offset).limit(limit)).asList(), offset, limit,
                false, uriInfo);
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
                                  @DefaultValue("false") @QueryParam("all") boolean all, @Context UriInfo uriInfo) {
        Query<Agent> query = agentDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        return new Batch<>(agentDAO.find(query).asList(), offset, limit, all, uriInfo);
    }

    @POST
    @Path("agent/{name}/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Test getTest(@PathParam("name") final String name, @PathParam("jobId") final String jobId) {
        Agent agent = agentDAO.findOne(agentDAO.createQuery().field("name").equal(name));
        if (agent == null) {
//            throw new BadRequestException("Unknown agent " + name);
            logger.error("bad request unknown agent {}", name);
            return null;
        }
        if (!jobId.equals(agent.getJobId())) {
//            throw new BadRequestException("Agent agent is not on job " + jobId + " " + agent);
            logger.error("Agent agent is not on job {} {} ", jobId, agent);
            return null;
        }
        agent.setLastTouchTime(new Date());
        Query<Test> query = testDAO.createQuery();
        query.and(query.criteria("jobId").equal(jobId), query.criteria("status").equal(Test.Status.PENDING));
        UpdateOperations<Test> updateOps = testDAO.createUpdateOperations().set("status", Test.Status.RUNNING)
                .set("assignedAgent", name).set("startTime", new Date());
        return testDAO.getDatastore().findAndModify(query, updateOps, false, false);
    }


    @GET
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Build> getBuilds(@DefaultValue("0") @QueryParam("offset") int offset,
                                  @DefaultValue("30") @QueryParam("limit") int limit,
                                  @DefaultValue("false") @QueryParam("all") boolean all, @Context UriInfo uriInfo) {
        Query<Build> query = buildDAO.createQuery();
        if (!all) {
            query.offset(offset).limit(limit);
        }
        return new Batch<>(buildDAO.find(query).asList(), offset, limit, all, uriInfo);
    }

    @PUT
    @Path("build")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build createBuild(final Build build) {
        buildDAO.save(build);
        return build;
    }

    @POST
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build updateBuild(final @PathParam("id") String id, final Build build) {
        build.setId(id);
        buildDAO.save(build);
        return build;
    }

    @GET
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Build getBuild(final @PathParam("id") String id) {
        return buildDAO.findOne(buildDAO.createQuery().field("_id").equal(new ObjectId(id)));
    }


    @POST
    @Path("build/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Build uploadBuild(final @PathParam("id") String id, final FormDataMultiPart multiPart) {

        InputStream is = multiPart.getField("testsuite-1.5.zip").getValueAs(InputStream.class);
        ZipInputStream zipInputStream = new ZipInputStream(is);
        logger.info("testsuite-1.5.zip stream is {}, build is {}", is, id);
        try {
            is.close();
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
        Build build = new Build();
        build.setId(id);
        return build;
    }

//    @POST
//    @Path("test/{id}")
//    public String updateTest(final FormDataMultiPart multiPart) {
//        //todo read the form and extract the log and the result.
//        InputStream is = multiPart.getField("my_pom").getValueAs(InputStream.class);
//        logger.info("my pom input stream is {}", is);
//        try {
//            is.close();
//        } catch (Exception e) {
//            logger.error(e.toString(), e);
//        }
//        String foo = multiPart.getField("foo").getValue();
//        logger.info("foo's value is {}", foo);
//        return "foo";
//    }


}
