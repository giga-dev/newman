package com.gigaspaces.componenets;

import com.gigaspaces.beans.PermResult;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Singleton
@Path("tests")
public interface TestsRepositoryIfc {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response ls(@DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("30") @QueryParam("limit") int limit, @Context UriInfo uriInfo);

    @POST
    @Path("perm")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    PermResult update(PermResult permResult);

    @PUT
    @Path("perm")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    PermResult create(PermResult permResult);

    @GET
    @Path("perm")
    @Produces(MediaType.APPLICATION_JSON)
    List<PermResult> lsPerms();

    @GET
    @Path("perm/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    PermResult get(@PathParam("id") String objectId);

    @POST
    @Path("post")
    void post(FormDataMultiPart multiPart);
}
