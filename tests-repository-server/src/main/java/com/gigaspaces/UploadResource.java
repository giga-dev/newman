package com.gigaspaces;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/10/15.
 */
public class UploadResource extends ResourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(UploadResource.class);

    public UploadResource() {
        super(MultiPartFeature.class);
    }


    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "Test";
    }

    @POST
    @Produces("multipart/mixed")
    @Path("files")
    public MultiPart post(final FormDataMultiPart multiPart) {
        return multiPart;
    }

}
