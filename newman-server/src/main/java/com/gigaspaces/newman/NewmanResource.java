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
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

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
        ds.ensureIndexes(); //creates all defined with @Indexed
        ds.ensureCaps(); //creates all collections for @Entity(cap=@CappedAt(...))
        jobDAO = new JobDAO(morphia, mongoClient, DB);
        testDAO = new TestDAO(morphia, mongoClient, DB);
    }

    @GET
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed("admin")
    public Batch<Job> jobs(@DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("30") @QueryParam("limit") int limit) {
        return new Batch<Job>(jobDAO.find(jobDAO.createQuery().offset(offset).limit(limit)).asList(), offset, limit);
    }

    @PUT
    @Path("job")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String update(JobRequest jobRequest) {
        Job job = new Job();

        jobDAO.save(job);
        return job.getId();
    }

    @GET
    @Path("job/{jobId}/test")
    @Consumes(MediaType.APPLICATION_JSON)
    public Batch<Test> tests(@PathParam("jobId") String jobId, @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("30") @QueryParam("limit") int limit) {
        return new Batch<Test>(testDAO.find(testDAO.createQuery().offset(offset).limit(limit)).asList(), offset, limit);
    }

    @POST
    @Path("test/{id}")
    public String updateTest(final FormDataMultiPart multiPart) {
        //todo read the form and extract the log and the result.
        InputStream is = multiPart.getField("my_pom").getValueAs(InputStream.class);
        logger.info("my pom input stream is {}", is);
        try {
            is.close();
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
        String foo = multiPart.getField("foo").getValue();
        logger.info("foo's value is {}", foo);
        return "foo";
    }


}
