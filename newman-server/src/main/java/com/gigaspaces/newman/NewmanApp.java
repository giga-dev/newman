package com.gigaspaces.newman;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.gigaspaces.newman.usermanagment.UserController;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.linking.DeclarativeLinkingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
@Singleton
@ApplicationPath("/")
public class NewmanApp extends ResourceConfig {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(NewmanApp.class);


    public NewmanApp() {
        register(SpotinstResource.class);
        register(NewmanResource.class);
        register(BroadcasterResource.class);
        register(ResourceListingResource.class);
        register(RolesAllowedDynamicFeature.class);
        register(MultiPartFeature.class);
        register(DeclarativeLinkingFeature.class);
        register(LoggingFilter.class);
        register(SseFeature.class);
        register(UserController.class);

//        property(ServerProperties.TRACING, "ALL");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper(mapper);
        register(jacksonProvider);
    }
}