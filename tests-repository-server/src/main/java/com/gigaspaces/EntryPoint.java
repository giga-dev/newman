package com.gigaspaces;

/**
 * Created by Barak Bar Orion
 * 3/1/15.
 */

import com.gigaspaces.beans.SuiteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("entry-point")
public class EntryPoint  {
    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);

    public EntryPoint() {
    }

    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public SuiteResult test() {
        return new SuiteResult();
    }

    @POST
    @Path("testJSON")
    @Produces(MediaType.APPLICATION_JSON)
    public SuiteResult testJSON() {
        return new SuiteResult();
    }

    @POST
    @Path("testJSON1")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public SuiteResult testJSON(String param) {
        return new SuiteResult();
    }

    @Path("upload")
    public UploadResource uploadResource(){
        return new UploadResource();
    }




}