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
import java.net.InetAddress;
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
        try {

            Build build = newmanClient.createBuild(new Build()).toCompletableFuture().get();
            logger.debug("create a new build {}", build);
            build = newmanClient.getBuild(build.getId()).toCompletableFuture().get();
            logger.debug("got build {}", build);
            JobRequest jobRequest = new JobRequest();
            jobRequest.setBuildId(build.getId());
            Job job = newmanClient.createJob(jobRequest).toCompletableFuture().get();
            logger.debug("creating new Job {}", job);
            Batch<Job> jobs = newmanClient.getJobs().toCompletableFuture().get();
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

            Agent agent = new Agent();
            agent.setName("foo");
            agent.setHost(InetAddress.getLocalHost().getCanonicalHostName());
            job = newmanClient.subscribe(agent).toCompletableFuture().get();
            logger.debug("agent {} subscribe to {}", agent.getName(), job);
            agent = newmanClient.getAgent(agent.getName()).toCompletableFuture().get();

            Batch<Agent> subscriptions = newmanClient.getSubscriptions(0, 100).toCompletableFuture().get();
            logger.debug("subscriptions {}", subscriptions);


            Test test = newmanClient.getReadyTest(agent.getName(), job.getId()).toCompletableFuture().get();
            logger.info("getReadyTest({}, {}) returns {}", agent.getName(), job.getId(), test);
            String jobId = newmanClient.ping(agent.getName(), job.getId(), test.getId()).toCompletableFuture().get();
            logger.debug("agent {} is working on job {}", agent.getName(), jobId);

        } catch (Exception e) {
            logger.error(e.toString(), e);
        } finally {
            newmanClient.close();
        }
    }

    public NewmanClient(RxClient<RxCompletionStageInvoker> restClient, String uri) {
        this.restClient = restClient;
        this.uri = uri;
    }

    public CompletionStage<Batch<Job>> getJobs(int offset, int limit) {
        return restClient.target(uri).path("job").queryParam("offset", offset).queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Job>>() {
                });
    }

    public CompletionStage<Batch<Job>> getJobs(int limit) {
        return restClient.target(uri).path("job").queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Job>>() {
                });
    }

    public CompletionStage<Batch<Job>> getJobs() {
        return restClient.target(uri).path("job").request().rx().get(new GenericType<Batch<Job>>() {
        });
    }

    public CompletionStage<Job> createJob(JobRequest jobRequest) {
        return restClient.target(uri).path("job").request().rx().put(Entity.json(jobRequest), Job.class);
    }

    public CompletionStage<Test> createTest(Test test) {
        return restClient.target(uri).path("test").request().rx().put(Entity.json(test), Test.class);
    }

    public CompletionStage<Test> updateTest(Test test) {
        return restClient.target(uri).path("test").request().rx().post(Entity.json(test), Test.class);
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


    public CompletionStage<Build> createBuild(Build build) {
        return restClient.target(uri).path("build").request().rx().put(Entity.json(build), Build.class);
    }

    public CompletionStage<Build> getBuild(String id) {
        return restClient.target(uri).path("build").path(id).request().rx().get(Build.class);
    }

    public CompletionStage<Job> subscribe(Agent agent) {
        return restClient.target(uri).path("subscribe").request().rx().post(Entity.json(agent), Job.class);
    }

    public CompletionStage<Batch<Agent>> getSubscriptions(int offset, int limit) {
        return restClient.target(uri).path("subscribe").queryParam("offset", offset).queryParam("limit", limit).request()
                .rx().get(new GenericType<Batch<Agent>>() {
                });
    }

    public CompletionStage<String> ping(String agentName, String jobId, String testId) {
        return restClient.target(uri).path("ping").path(agentName).path(jobId).path(testId).request().rx().get(String.class);
    }

    public CompletionStage<Agent> getAgent(String agentName) {
        return restClient.target(uri).path("agent").path(agentName).request().rx().get(Agent.class);
    }

    public CompletionStage<Test> getReadyTest(String agentName, String jobId) {
        return restClient.target(uri).path("agent").path(agentName).path(jobId).request().rx().post(Entity.text(""), Test.class);
    }

    public void close() {
        restClient.close();
    }

    public static NewmanClient create(String user, String pw) {
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
                .register(HttpAuthenticationFeature.basic(user, pw));

        RxClient<RxCompletionStageInvoker> restClient = RxCompletionStage.from(jerseyClientBuilder.build());
        return new NewmanClient(restClient, URI);
    }
}
