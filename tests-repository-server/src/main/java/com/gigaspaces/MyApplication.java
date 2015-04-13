package com.gigaspaces;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.linking.DeclarativeLinkingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.ApplicationPath;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
@ApplicationPath("/")
public class MyApplication extends ResourceConfig {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(MyApplication.class);

    public MyApplication() {
        super(TestsRepository.class, EntryPoint.class, BroadcasterResource.class, MultiPartFeature.class, DeclarativeLinkingFeature.class, LoggingFilter.class);
    }
}