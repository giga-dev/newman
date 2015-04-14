package com.gigaspaces;


import com.gigaspaces.beans.Batch;
import com.gigaspaces.beans.PermResult;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.sse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by Barak Bar Orion
 * <p/>
 * 4/7/15.
 */
public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    public static void main(String[] args) {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            ExecutorService executor = Executors.newCachedThreadPool();


            JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder()
                    .register(MultiPartFeature.class).register(SseFeature.class)
                    .register(HttpAuthenticationFeature.basic("root", "root"));

            Client client = jerseyClientBuilder.build();
            WebTarget target = client.target("http://localhost:8080/api/tests/perm");
            PermResult permResult = new PermResult();
            permResult.setPermutation(UUID.randomUUID().toString());
            PermResult result = target.request().post(Entity.json(permResult), PermResult.class);
            logger.info("post result : {}", result);

            // subscribe to broadcast.
            target = client.target("http://localhost:8080/api/broadcast");
            EventInput eventInput = target.request().get().readEntity(EventInput.class);
            executor.execute(() -> {
                while (!eventInput.isClosed()) {
                    InboundEvent event = eventInput.read();
                    if (event == null) {
                        return;
                    }
                    logger.info("Event: {} {}", event.getName(), event.readData(String.class));
                }
            });
            // register listener only to events named "message-to-client".
            EventSource eventSource = EventSource.target(target).build();
            EventListener listener = inboundEvent ->
                    logger.info("listener got event: {} {}", inboundEvent.getName(), inboundEvent.readData(String.class));
            eventSource.register(listener, "message-to-client");
            eventSource.open();

            // sending event.
            String message = target.request().post(Entity.text("*foo*"), String.class);

            logger.info("client broadcast {}", message);

            target = client.target("http://localhost:8080/api/tests");
            Response response = target.request().get();
            response.bufferEntity();
            Batch batch = response.readEntity(Batch.class);
            logger.info("all ids are : {}", batch);


            final FileDataBodyPart filePart = new FileDataBodyPart("my_pom", new File("pom.xml"));

            final MultiPart multipart = new FormDataMultiPart()
                    .field("foo", "bar")
                    .bodyPart(filePart);
//
            target = client.target("http://localhost:8080/api/tests/post");
            final Response res = target.request()
                    .post(Entity.entity(multipart, multipart.getMediaType()));
            logger.info("response {}", res);


        } catch (Exception e) {

            e.printStackTrace();

        }

    }
}
