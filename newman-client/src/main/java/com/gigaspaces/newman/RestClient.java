package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.JobRequest;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.RxWebTarget;
import org.glassfish.jersey.client.rx.java8.RxCompletionStage;
import org.glassfish.jersey.client.rx.java8.RxCompletionStageInvoker;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.sse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by Barak Bar Orion
 * <p>
 * 4/7/15.
 */
public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    public static void main(String[] args) {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            ExecutorService executor = Executors.newCachedThreadPool();

            SslConfigurator sslConfig = SslConfigurator.newInstance()
                    .trustStoreFile("keys/server.keystore")
                    .trustStorePassword("password")
                    .keyStoreFile("keys/server.keystore")
                    .keyPassword("password");
//
            SSLContext sslContext = sslConfig.createSSLContext();
            JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder()
                    .sslContext(sslContext)
                    .hostnameVerifier((s, sslSession) -> true)
                    .register(MultiPartFeature.class).register(SseFeature.class)
                    .register(HttpAuthenticationFeature.basic("root", "root"));

            RxClient<RxCompletionStageInvoker> client = RxCompletionStage.from(jerseyClientBuilder.build());
            RxWebTarget<RxCompletionStageInvoker> target = client.target("https://localhost:8443/api/newman/job");

            String jobId = target.request().put(Entity.json(new JobRequest()), String.class);
            logger.info("added job {} ", jobId);

            Batch<Job> jobs = target.request().get(new GenericType<Batch<Job>>() {});
            logger.info("query jobs returns: {}", jobs);

//            query jobs async
            target.request().rx().get(new GenericType<Batch<Job>>() {
            }).thenAccept(jobBatch -> logger.info("async query jobs returns: {}", jobs)).exceptionally(throwable -> {
                logger.error(throwable.toString(), throwable);
                return null;
            });

            // subscribe to broadcast.
            target = client.target("https://localhost:8443/api/broadcast");
            EventInput eventInput = target.request().get().readEntity(EventInput.class);
            executor.execute(() -> {
                //noinspection LoopStatementThatDoesntLoop
                while (!eventInput.isClosed()) {
                    InboundEvent event = eventInput.read();
                    if (event == null) {
                        return;
                    }
                    logger.info("Event: {} {}", event.getName(), event.readData(String.class));
                    return;
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


            final FileDataBodyPart filePart = new FileDataBodyPart("my_pom", new File("pom.xml"));

            final MultiPart multipart = new FormDataMultiPart()
                    .field("foo", "bar")
                    .bodyPart(filePart);

            target = client.target("https://localhost:8443/api/newman/test/id");
            final Response res = target.request()
                    .post(Entity.entity(multipart, multipart.getMediaType()));
            logger.info("response {}", res);
            eventInput.close();
            eventSource.close();
            client.close();
            logger.info("eventSource is open {}", eventSource.isOpen());
            executor.shutdownNow();
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }

    }
}
