package com.gigaspaces.newman;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by Barak Bar Orion
 * 5/14/15.
 */
@Singleton
@Path("/resources")
public class ResourceListingResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response showAll(@Context Application application, @Context HttpServletRequest request) {
        String basePath = request.getRequestURL().toString();

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode resources = JsonNodeFactory.instance.arrayNode();

        root.set("resources", resources);

        //                for (Resource srm : resource.getChildResources()) {
//                    String uri = uriPrefix + "/" + srm.getPath();
//                    addTo(resourceNode, uri, srm, joinUri(basePath, uri));
//                }
        application.getClasses().stream().filter(this::isAnnotatedResourceClass).forEach(aClass -> {
            Resource resource = Resource.builder(aClass).build();
            ObjectNode resourceNode = JsonNodeFactory.instance.objectNode();
            String uriPrefix = resource.getPath();

            for (Resource r : resource.getChildResources()) {
                String uri = uriPrefix + "/" + r.getPath();
//                addTo(resourceNode, uri, r, joinUri(basePath, uri));
            }

            for (ResourceMethod srm : resource.getResourceMethods()) {
                addTo(resourceNode, uriPrefix, srm, joinUri(basePath, uriPrefix));
            }

            resources.add(resourceNode);
        });
        return Response.ok().entity(root).build();
    }

    private void addTo(ObjectNode resourceNode, String uriPrefix, ResourceMethod srm, String path) {
        if (resourceNode.get(uriPrefix) == null) {
            ObjectNode inner = JsonNodeFactory.instance.objectNode();
            inner.put("path", path);
            inner.set("verbs", JsonNodeFactory.instance.arrayNode());
            resourceNode.set(uriPrefix, inner);
        }

        ((ArrayNode) resourceNode.get(uriPrefix).get("verbs")).add(srm.getHttpMethod());
    }


    private boolean isAnnotatedResourceClass(Class rc) {
        if (rc.isAnnotationPresent(Path.class)) {
            return true;
        }

        for (Class i : rc.getInterfaces()) {
            if (i.isAnnotationPresent(Path.class)) {
                return true;
            }
        }

        return false;
    }

    public static String joinUri(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0 && result.charAt(result.length() - 1) == '/') {
                result.setLength(result.length() - 1);
            }
            if (result.length() > 0 && !part.startsWith("/")) {
                result.append('/');
            }
            result.append(part);
        }
        return result.toString();
    }


}