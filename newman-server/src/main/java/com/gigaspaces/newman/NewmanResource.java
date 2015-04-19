package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.dao.JobDAO;
import com.gigaspaces.newman.dao.TestDAO;
import com.mongodb.MongoClient;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
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
    private final static String DB = "db";
    private final MongoClient mongoClient;
    private final Morphia morphia;
    private final Datastore ds;
    private final JobDAO jobDAO;
    private final TestDAO testDAO;

    public NewmanResource() {
        mongoClient = new MongoClient("localhost");
        morphia = new Morphia().map(Job.class, Agent.class, Build.class, Test.class);
        ds = morphia.createDatastore(mongoClient, DB);
        ds.ensureIndexes();
        ds.ensureCaps();
        jobDAO = new JobDAO(morphia, mongoClient, DB);
        testDAO = new TestDAO(morphia, mongoClient, DB);
    }

    @GET
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<Job> jobs(@DefaultValue("0") @QueryParam("offset") int offset,
                           @DefaultValue("30") @QueryParam("limit") int limit, @Context UriInfo uriInfo) {
        return new Batch<>(jobDAO.find(jobDAO.createQuery().offset(offset).limit(limit)).asList(), offset, limit
                , uriInfo);
    }

    @PUT
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Job update(JobRequest jobRequest) {
        Job job = new Job();
        jobDAO.save(job);
        return job;
    }


    @PUT
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Test addTest(Test test) {
        testDAO.save(test);
        return test;
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Batch<Test> tests(@DefaultValue("0") @QueryParam("offset") int offset,
                             @DefaultValue("30") @QueryParam("limit") int limit, @QueryParam("jobId") String jobId, @Context UriInfo uriInfo) {
        logger.info("jobId is {}", jobId);
        return new Batch<>(testDAO.find(testDAO.createQuery().field("jobId").equal(jobId).offset(offset).limit(limit)).asList(), offset, limit
                , uriInfo);
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
