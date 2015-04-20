package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.java8.RxCompletionStage;
import org.glassfish.jersey.client.rx.java8.RxCompletionStageInvoker;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import java.util.concurrent.CompletionStage;

/**
 * Created by Barak Bar Orion
 * 4/19/15.
 */
public class NewmanClient {
    private static final Logger logger = LoggerFactory.getLogger(NewmanClient.class);
    private final RxClient<RxCompletionStageInvoker> restClient;
    private final String uri;

    public static void main(String[] args) {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            final String URI = "https://localhost:8443/api/newman";
            SslConfigurator sslConfig = SslConfigurator.newInstance()
                    .trustStoreFile("keys/server.keystore")
                    .trustStorePassword("password")
                    .keyStoreFile("keys/server.keystore")
                    .keyPassword("password");

            SSLContext sslContext = sslConfig.createSSLContext();
            JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder()
                    .sslContext(sslContext)
                    .hostnameVerifier((s, sslSession) -> true)
                    .register(MultiPartFeature.class).register(SseFeature.class)
                    .register(HttpAuthenticationFeature.basic("root", "root"));

            RxClient<RxCompletionStageInvoker> restClient = RxCompletionStage.from(jerseyClientBuilder.build());
            NewmanClient newmanClient = new NewmanClient(restClient, URI);
            Job job = newmanClient.createJob(new JobRequest()).toCompletableFuture().get();
            logger.debug("creating new Job {}", job);
            Batch<Job> jobs = newmanClient.jobs().toCompletableFuture().get();
            logger.debug("jobs are: {}", jobs);
            for (int i = 0; i < 100; i++) {
                Test test = new Test();
                test.setJobId(job.getId());
                test.setName("test_" + i);
                test = newmanClient.createTest(test).toCompletableFuture().get();
                logger.debug("added test {}", test);
            }
            Batch<Test> tests = newmanClient.getTests(job.getId(), 0, 30).toCompletableFuture().get();
            logger.debug("tests are {}", tests);
            for (Test test : tests.getValues()) {
                Test t = newmanClient.getTest(test.getId()).toCompletableFuture().get();
                logger.debug("read test by id {}, {}", test.getId(), t);

            }
            Build build = newmanClient.createBuild(new Build()).toCompletableFuture().get();
            logger.debug("create a new build {}", build);
            build = newmanClient.getBuild(build.getId()).toCompletableFuture().get();
            logger.debug("got build {}", build);
            Agent agent = new Agent();
            agent.setId("foo");
            build = newmanClient.subscribe(agent).toCompletableFuture().get();
            logger.debug("agent {} subscribe to {}", agent.getId(), build);
        } catch (Exception e) {
            logger.error(e.toString(), e);
        }
    }

    public NewmanClient(RxClient<RxCompletionStageInvoker> restClient, String uri) {
        this.restClient = restClient;
        this.uri = uri;
    }

    public CompletionStage<Batch<Job>> jobs(int offset, int limit) {
        return restClient.target(uri).path("job").queryParam("offset", offset).queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Job>>() {
                });
    }

    public CompletionStage<Batch<Job>> jobs(int limit) {
        return restClient.target(uri).path("job").queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Job>>() {
                });
    }

    public CompletionStage<Batch<Job>> jobs() {
        return restClient.target(uri).path("job").request().rx().get(new GenericType<Batch<Job>>() {
        });
    }

    public CompletionStage<Job> createJob(JobRequest jobRequest) {
        return restClient.target(uri).path("job").request().rx().put(Entity.json(jobRequest), Job.class);
    }

    public CompletionStage<Test> createTest(Test test) {
        return restClient.target(uri).path("test").request().rx().put(Entity.json(test), Test.class);
    }

    public CompletionStage<Batch<Test>> getTests(String jobId, int offset, int limit) {
        return restClient.target(uri).path("test").queryParam("offset", offset).queryParam("limit", limit)
                .queryParam("jobId", jobId).request()
                .rx().get(new GenericType<Batch<Test>>() {
                });
    }

    public CompletionStage<Test> getTest(String id) {
        return restClient.target(uri).path("test").path(id).request().rx().get(Test.class);
    }


    public CompletionStage<Build> createBuild(Build build){
        return restClient.target(uri).path("build").request().rx().put(Entity.json(build), Build.class);
    }

    public CompletionStage<Build> getBuild(String id){
        return restClient.target(uri).path("build").path(id).request().rx().get(Build.class);
    }

    //todo continue implement

    public CompletionStage<Build> subscribe(Agent agent) {
        return restClient.target(uri).path("agent").request().rx().post(Entity.json(agent), Build.class);
    }

    public CompletionStage<Void> ping(String agentId) {
        return restClient.target(uri).path("agent").path(agentId).request().rx().get(Void.class);
    }

    public CompletionStage<Build> build(String id) {
        return restClient.target(uri).path("build").path(id).request().rx().get(Build.class);
    }


    public CompletionStage<Test> get(String agentId) {
        return restClient.target(uri).path("test").request().rx().post(Entity.text(agentId), Test.class);
    }

    public void close() {
        restClient.close();
    }
}
