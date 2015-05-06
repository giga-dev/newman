package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.PatternCriteria;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.java8.RxCompletionStage;
import org.glassfish.jersey.client.rx.java8.RxCompletionStageInvoker;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.UUID;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(NewmanClient.class);
    
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
            EventInput eventInput = newmanClient.getEventInput();
            logger.info("got eventInput {}", eventInput);
            eventInput.close();
            Suite suite = new Suite();
            suite.setName("full regression");
            suite.setCriteria(PatternCriteria.recursivePackageNameCriteria("com.gigaspaces.test"));

            suite = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("suite is {}", suite);
            Batch<Suite> suites = newmanClient.getAllSuites().toCompletableFuture().get();
            logger.info("all suites {}", suites);
            suite = newmanClient.getSuite(suite.getId()).toCompletableFuture().get();
            logger.info("got suite {}", suite);

            Build build = new Build();
            build.setName("The build " + UUID.randomUUID());
            build.setBranch("master");
            build = newmanClient.createBuild(build).toCompletableFuture().get();
            logger.info("create a new build {}", build);
            build = newmanClient.getBuild(build.getId()).toCompletableFuture().get();
            logger.info("got build {}", build);
            JobRequest jobRequest = new JobRequest();
            jobRequest.setBuildId(build.getId());
            jobRequest.setSuiteId(suite.getId());

            Job job = newmanClient.createJob(jobRequest).toCompletableFuture().get();
            logger.info("creating new Job {}", job);
            Batch<Job> jobs = newmanClient.getJobs().toCompletableFuture().get();
            logger.info("jobs are: {}", jobs);
            for (int i = 0; i < 100; i++) {
                Test test = new Test();
                test.setJobId(job.getId());
                test.setName("test_" + i);
                test = newmanClient.createTest(test).toCompletableFuture().get();
                logger.info("added test {}", test);
                test = newmanClient.uploadLog(test.getId(), new File("mongo.txt")).toCompletableFuture().get();
                logger.info("**** Test is {} ", test);
            }
            Batch<Test> tests = newmanClient.getTests(job.getId(), 0, 30).toCompletableFuture().get();
            logger.info("tests are {}", tests);
//            for (Test test : tests.getValues()) {
//                Test t = newmanClient.getTest(test.getId()).toCompletableFuture().get();
//                logger.info("read test by id {}, {}", test.getId(), t);
//            }


            Agent agent = new Agent();
            agent.setName("foo");
            agent.setHost(InetAddress.getLocalHost().getCanonicalHostName());
            job = newmanClient.subscribe(agent).toCompletableFuture().get();
            logger.info("agent {} subscribe to {}", agent.getName(), job);
            agent = newmanClient.getAgent(agent.getName()).toCompletableFuture().get();

            Batch<Agent> subscriptions = newmanClient.getSubscriptions(0, 100).toCompletableFuture().get();
            logger.info("subscriptions {}", subscriptions);


//            Test test = newmanClient.getReadyTest(agent.getName(), job.getId()).toCompletableFuture().get();
//            logger.info("getReadyTest({}, {}) returns {}", agent.getName(), job.getId(), test);
//            String jobId = newmanClient.ping(agent.getName(), job.getId(), test.getId()).toCompletableFuture().get();
//            logger.info("agent {} is working on job {}", agent.getName(), jobId);
//
//            tests = newmanClient.getTests(job.getId(), 0, 1000).toCompletableFuture().get();
//            logger.info("tests are {}", tests);
            int i = 0;
            while (true) {
                Test test = newmanClient.getReadyTest("foo", job.getId()).toCompletableFuture().get();
                logger.info("agent took test {}", test);
                if (test == null) {
                    break;
                }
                if (i % 2 == 0) {
                    test.setStatus(Test.Status.SUCCESS);
                    newmanClient.finishTest(test).toCompletableFuture().get();
                    logger.info("SUCCESS test {}", test);
                } else {
                    test.setStatus(Test.Status.FAIL);
                    test.setErrorMessage(new IllegalArgumentException().toString());
                    newmanClient.finishTest(test).toCompletableFuture().get();
                    logger.info("FAIL test {}", test);
                }
                i += 1;
            }


        } catch (Exception e) {
            logger.error(e.toString(), e);
        } finally {
            newmanClient.close();
        }
    }
}
