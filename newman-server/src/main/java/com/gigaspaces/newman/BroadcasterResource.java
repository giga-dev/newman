package com.gigaspaces.newman;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ChunkedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Singleton
@Path("broadcast")
public class BroadcasterResource {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(BroadcasterResource.class);

    private final SseBroadcaster broadcaster;

    public BroadcasterResource() {
        this.broadcaster = new SseBroadcaster(){
            @Override
            public void onException(ChunkedOutput<OutboundEvent> chunkedOutput, Exception exception) {
                logger.error(exception.toString(), exception);
                remove(chunkedOutput);
            }

            @Override
            public void onClose(ChunkedOutput<OutboundEvent> chunkedOutput) {
                remove(chunkedOutput);
            }
        };
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String broadcastMessage(String message) {
        OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        OutboundEvent event = eventBuilder.name("message")
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .data(String.class, message)
                .build();

        broadcaster.broadcast(event);

        event = eventBuilder.name("message-to-client")
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .data(String.class, message)
                .build();
        broadcaster.broadcast(event);
        broadcaster.closeAll();
        return "Message '" + message + "' has been broadcast.";
    }

    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput listenToBroadcast() {
        logger.info("addListener");
        final EventOutput eventOutput = new EventOutput();
        this.broadcaster.add(eventOutput);
        return eventOutput;
    }
}