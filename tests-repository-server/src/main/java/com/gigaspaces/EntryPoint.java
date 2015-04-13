package com.gigaspaces;

/**
 * Created by Barak Bar Orion
 * 3/1/15.
 */

import com.gigaspaces.beans.Result;
import com.mongodb.MongoClient;
import org.glassfish.jersey.media.multipart.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Path("entry-point")
public class EntryPoint  {
    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);

    public EntryPoint() {
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Result test() {
        return new Result("foo");
    }

    @POST
    @Path("testJSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Result testJSON() {
        return new Result("foo");
    }

    @POST
    @Path("testJSON1")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Result testJSON(String param) {
        return new Result(param);
    }

    @Path("upload")
    public UploadResource uploadResource(){
        return new UploadResource();
    }




}