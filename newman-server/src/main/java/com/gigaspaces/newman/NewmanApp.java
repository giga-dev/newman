package com.gigaspaces.newman;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.linking.DeclarativeLinkingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
@ApplicationPath("/")
public class NewmanApp extends ResourceConfig {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(NewmanApp.class);



    public NewmanApp() {
        super(NewmanResource.class, BroadcasterResource.class, RolesAllowedDynamicFeature.class,
                MultiPartFeature.class, DeclarativeLinkingFeature.class, LoggingFilter.class, SseFeature.class);
//        property(ServerProperties.TRACING, "ALL");
    }
}