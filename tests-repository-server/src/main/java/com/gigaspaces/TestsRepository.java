package com.gigaspaces;

import com.gigaspaces.beans.Batch;
import com.gigaspaces.beans.PermResult;
import com.gigaspaces.componenets.TestsRepositoryIfc;
import com.gigaspaces.dao.PermResultDAO;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.QueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
@Singleton
@Path("tests")
public class TestsRepository implements TestsRepositoryIfc {

    private static final Logger logger = LoggerFactory.getLogger(TestsRepository.class);
    private final static String DB = "db";
    private final MongoClient mongoClient;
    private final Morphia morphia;
    private final Datastore ds;
    private final PermResultDAO permResultDAO;

    public TestsRepository() {
        mongoClient = new MongoClient("localhost");
        morphia = new Morphia().map(PermResult.class);
        ds = morphia.createDatastore(mongoClient, DB);
        ds.ensureIndexes(); //creates all defined with @Indexed
        ds.ensureCaps(); //creates all collections for @Entity(cap=@CappedAt(...))
        permResultDAO = new PermResultDAO(morphia, mongoClient, DB);
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response ls(@DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("30") @QueryParam("limit") int limit, @Context UriInfo uriInfo) {
        return Response.ok().entity(new Batch<String>(permResultDAO.findIds(permResultDAO.createQuery().offset(offset).limit(limit)).stream().map(ObjectId::toString).collect(Collectors.toList()), offset, limit)).build();
    }

    @Override
    @POST
    @Path("perm")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PermResult update(PermResult permResult) {
        permResultDAO.save(permResult);
        return permResult;
    }

    @Override
    @PUT
    @Path("perm")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PermResult create(PermResult permResult) {
        permResultDAO.save(permResult);
        return permResult;
    }

    @Override
    @GET
    @Path("perm")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PermResult> lsPerms() {
        return permResultDAO.find().asList();
    }

    @Override
    @GET
    @Path("perm/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PermResult get(@PathParam("id") String objectId) {
        return permResultDAO.get(new ObjectId(objectId));
    }

    @Override
    @POST
    @Path("post")
    public void post(final FormDataMultiPart multiPart) {
        InputStream is = multiPart.getField("my_pom").getValueAs(InputStream.class);
        logger.info("my pom input stream is {}", is);
        try {
            is.close();
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
        String foo = multiPart.getField("foo").getValue();
        logger.info("foo's value is {}", foo);
    }

}
