package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.PatternCriteria;
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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(NewmanClient.class);

    public static NewmanClient createNewmanClient() throws KeyManagementException, NoSuchAlgorithmException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        final String URI = "https://localhost:8443/api/newman";

        JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder()

                .sslContext(SSLContextFactory.acceptAll())
                .hostnameVerifier((s, sslSession) -> true)
                .register(MultiPartFeature.class).register(SseFeature.class)
                .register(HttpAuthenticationFeature.basic("root", "root"));

        RxClient<RxCompletionStageInvoker> restClient = RxCompletionStage.from(jerseyClientBuilder.build());

        return new NewmanClient(restClient, URI);
    }

    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException {

        NewmanClient newmanClient = createNewmanClient();
        try {
            EventInput eventInput = newmanClient.getEventInput();
            logger.info("got eventInput {}", eventInput);
            eventInput.close();

            Build build = new Build();
            build.setName("The build " + UUID.randomUUID());
            build.setBranch("master");
            build = newmanClient.createBuild(build).toCompletableFuture().get();
            logger.info("create a new build {}", build);
            build = newmanClient.getBuild(build.getId()).toCompletableFuture().get();
            logger.info("got build {}", build);
            Suite suite = null;
            Batch<Suite> suiteBatch = newmanClient.getAllSuites().toCompletableFuture().get();
            if (!suiteBatch.getValues().isEmpty()) {
                suite = suiteBatch.getValues().get(0);
            }
            createAndRunJob(newmanClient, suite, build);
            Thread.sleep(1000);
            createAndRunJob(newmanClient, suite, build);




        } catch (Exception e) {
            logger.error(e.toString(), e);
        } finally {
            newmanClient.close();
        }
    }

    private static void createAndRunJob(NewmanClient newmanClient, Suite suite, Build build) throws InterruptedException, java.util.concurrent.ExecutionException, UnknownHostException {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setBuildId(build.getId());
        jobRequest.setSuiteId(suite.getId());

        Job job = newmanClient.createJob(jobRequest).toCompletableFuture().get();
        logger.info("creating new Job {}", job);
//        Batch<Job> jobs = newmanClient.getJobs().toCompletableFuture().get();
//        logger.info("jobs are: {}", jobs);
        for (int i = 0; i < 3000; i++) {
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


        Agent agent = new Agent();
        agent.setName("foo");
        agent.setHost(InetAddress.getLocalHost().getCanonicalHostName());
        job = newmanClient.subscribe(agent).toCompletableFuture().get();
        logger.info("agent {} subscribe to {}", agent.getName(), job);
        if(job == null){
            return;
        }
//            agent = newmanClient.getAgent(agent.getName()).toCompletableFuture().get();
//
        int i = 0;
        while (true) {
//                Test test = newmanClient.getTestToRun(agent).toCompletableFuture().get();
            Test test = newmanClient.getReadyTest("foo", job.getId()).toCompletableFuture().get();
            logger.info("agent took test {}", test);
            if (test == null) {
                break;
            }
//                Thread.sleep(100);
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
            if(i % 50 == 0){
                DashboardData dashboard = newmanClient.getDashboard().toCompletableFuture().get();
                logger.info("----------------- dashboard data is {}", dashboard);
            }
            Thread.sleep(500);
            i += 1;
        }
    }
}
