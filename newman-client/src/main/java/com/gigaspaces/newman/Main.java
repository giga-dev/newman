package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.DashboardData;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.JobRequest;
import com.gigaspaces.newman.beans.Suite;
import com.gigaspaces.newman.beans.Test;
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
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(NewmanClient.class);

    private static int NUMBER_OF_BUILDS = 2;
    private static int NUMBER_OF_SUITES_PER_BUILD = 3;
    private static int NUMBER_OF_JOBS_PER_SUITE = 6;
    private static long DELAY_BETWEEN_TESTS_MS = 10;

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

            Random random = new Random(System.currentTimeMillis());
            int buildNum = random.nextInt(100);
            for (int b=0; b<NUMBER_OF_BUILDS; b++) {

                Build build = new Build();
                build.setName("13504-"+(buildNum++));
                build.setBranch("master");
                build.setBuildTime(new Date());

                CompletionStage<Build> completionStage = newmanClient.createBuild(build);
                build = completionStage.toCompletableFuture().get();

                logger.info("create a new build {}", build);
                build = newmanClient.getBuild(build.getId()).toCompletableFuture().get();
                logger.info("got build {}", build);
                createAndRunJob(newmanClient, build);
            }



        } catch (Exception e) {
            logger.error(e.toString(), e);
        } finally {
            newmanClient.close();
        }
    }

    private static void createAndRunJob(NewmanClient newmanClient, Build build) throws InterruptedException, java.util.concurrent.ExecutionException, UnknownHostException {

        for (int s=0; s< NUMBER_OF_SUITES_PER_BUILD; s++) {

            Suite mySuite = new Suite();
            mySuite.setName("Suite-" + UUID.randomUUID().toString().substring(0, 3));
            Suite newSuite = newmanClient.addSuite(mySuite).toCompletableFuture().get();

            for (int j=0; j < NUMBER_OF_JOBS_PER_SUITE; j++) {
                JobRequest jobRequest = new JobRequest();
                jobRequest.setBuildId(build.getId());
                jobRequest.setSuiteId(newSuite.getId());
                newmanClient.createJob(jobRequest).toCompletableFuture().get();
            }
        }

        Batch<Job> jobBatch = newmanClient.getJobs().toCompletableFuture().get();
        List<Job> values = jobBatch.getValues();
        logger.info("number of jobs: " + values.size());
        for (Job job : values) {
            if (job.getTotalTests() != 0) {
                continue; //ignore job with tests
            }
            for (int i = 0; i < 3*10; i++) {
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
        }

        Agent agent = new Agent();
        agent.setName("foo");
        agent.setHost(InetAddress.getLocalHost().getCanonicalHostName());

        while(true) {
            Job job = newmanClient.subscribe(agent).toCompletableFuture().get();
            logger.info("agent {} subscribe to {}", agent.getName(), job);
            if (job == null) {
                return;
            }
            Random rand = new Random(System.currentTimeMillis());
            int i = 0;
            while (true) {
                Test test = newmanClient.getReadyTest("foo", job.getId()).toCompletableFuture().get();
                logger.info("agent took test {}", test);
                if (test == null) {
                    break;
                }
                if (rand.nextInt() % 9 != 0) {
                    test.setStatus(Test.Status.SUCCESS);
                    newmanClient.finishTest(test).toCompletableFuture().get();
                    logger.info("SUCCESS test {}", test);
                } else {
                    test.setStatus(Test.Status.FAIL);
                    test.setErrorMessage(new IllegalArgumentException().toString());
                    newmanClient.finishTest(test).toCompletableFuture().get();
                    logger.info("FAIL test {}", test);
                }
                if (i % 50 == 0) {
                    DashboardData dashboard = newmanClient.getDashboard().toCompletableFuture().get();
                    logger.info("----------------- dashboard data is {}", dashboard);
                }
                Thread.sleep(DELAY_BETWEEN_TESTS_MS);
                i += 1;
            }
        }
    }
}
