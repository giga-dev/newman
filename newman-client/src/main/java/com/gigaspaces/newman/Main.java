package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Build;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(NewmanClient.class);

    private final static int NUMBER_OF_BUILDS = 1;
    private final static int NUMBER_OF_SUITES_PER_BUILD = 1;
    private final static int NUMBER_OF_JOBS_PER_SUITE = 1;
    private final static int NUMBER_OF_TESTS_PER_JOB = 20;
    private final static long DELAY_BETWEEN_TESTS_MS = 1000;
    private final static long TEST_PROCESS_TIME_MS = 1000;
    private final static long PREPARE_JOB_TIME_MS = 5000;
    private final static int AGENT_THREADS = 2;

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
            for (int b = 0; b < NUMBER_OF_BUILDS; b++) {

                Build build = new Build();
                build.setName("13504-" + (buildNum++));
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

        for (int s = 0; s < NUMBER_OF_SUITES_PER_BUILD; s++) {

            Suite mySuite = new Suite();
            mySuite.setName("Suite-" + UUID.randomUUID().toString().substring(0, 3));
            Suite newSuite = newmanClient.addSuite(mySuite).toCompletableFuture().get();

            for (int j = 0; j < NUMBER_OF_JOBS_PER_SUITE; j++) {
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
            String jobId = job.getId();
            for (int i = 0; i < NUMBER_OF_TESTS_PER_JOB; i++) {
                Test test = new Test();
                test.setJobId(jobId);
                test.setName("test_" + i);
                test.setArguments(Arrays.asList(Test.class.getName()/*, "arg1", "arg2" */));
                test = newmanClient.createTest(test).toCompletableFuture().get();
                logger.info("added test {}", test);
                test = newmanClient.uploadLog( jobId, test.getId(), new File("mongo.txt")).toCompletableFuture().get();
                logger.info("**** Test is {} ", test);
            }
            Batch<Test> tests = newmanClient.getTests(job.getId(), 0, NUMBER_OF_TESTS_PER_JOB).toCompletableFuture().get();
            logger.info("tests are {}", tests);
        }

        Agent foo1Agent = new Agent();
        foo1Agent.setName("foo1");
        foo1Agent.setHostAddress(InetAddress.getLocalHost().getHostAddress());
        foo1Agent.setPid(String.valueOf(1234));
        foo1Agent.setHost(InetAddress.getLocalHost().getCanonicalHostName());
        foo1Agent.setHostAddress(InetAddress.getLocalHost().getHostAddress());
        foo1Agent.setPid("123456");

        Agent foo2Agent = new Agent();
        foo2Agent.setName("foo2");
        foo2Agent.setHostAddress(InetAddress.getLocalHost().getHostAddress());
        foo2Agent.setPid(String.valueOf(1111));
        foo2Agent.setHost(InetAddress.getLocalHost().getCanonicalHostName());
        foo2Agent.setHostAddress(InetAddress.getLocalHost().getHostAddress());
        foo2Agent.setPid("123");

        int index = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            index++;

            CompletionStage<Job> subscribeToAgent1 = newmanClient.subscribe(foo1Agent);
            CompletableFuture<Job> jobCompletableFuture1 = subscribeToAgent1.toCompletableFuture();

            CompletionStage<Job> subscribeToAgent2 = newmanClient.subscribe(foo2Agent);
            CompletableFuture<Job> jobCompletableFuture2 = subscribeToAgent2.toCompletableFuture();

            Job job1 = jobCompletableFuture1.get();
            Job job2 = jobCompletableFuture2.get();

            logger.info("agent {} subscribe to {}", foo1Agent.getName(), job1);
            if (job1 == null ) {
                Thread.sleep(1000);
                // continue to try maybe there are paused job or there will be new job some time later.
                continue;
            } else {
                logger.info("agent {} preparing folder for processing {}, it should take {} millis", foo1Agent.getName(), job1, PREPARE_JOB_TIME_MS);
                Thread.sleep(PREPARE_JOB_TIME_MS);
            }

            if (job2 == null ) {
                Thread.sleep(1000);
                // continue to try maybe there are paused job or there will be new job some time later.
                continue;
            } else {
                logger.info("agent {} preparing folder for processing {}, it should take {} millis", foo2Agent.getName(), job2, PREPARE_JOB_TIME_MS);
                Thread.sleep(PREPARE_JOB_TIME_MS);
            }


            //noinspection InfiniteLoopStatement
            while (true) {

                Agent agent = ( new Random(System.currentTimeMillis()).nextInt()%2 == 0 ) ? foo1Agent : foo2Agent;

                List<Test> tests = takeTests(newmanClient, agent.getName(), job1.getId());
                if(tests.isEmpty()){
                    break;
                }
                int threadNumber = 0;
                for (Test test : tests) {

                    logger.info("agent {} processing test {}", agent.getName() + ":" + threadNumber, test);
                    Thread.sleep(TEST_PROCESS_TIME_MS);
                    if ( new Random(System.currentTimeMillis()).nextInt() % 9 != 0) {
                        test.setStatus(Test.Status.SUCCESS);
                        newmanClient.finishTest(test).toCompletableFuture().get();
                        logger.info("agent {} SUCCESS test {}", agent.getName()  + ":" + threadNumber, test);
                    } else {
                        test.setStatus(Test.Status.FAIL);
                        test.setErrorMessage(new IllegalArgumentException().toString());
                        newmanClient.finishTest(test).toCompletableFuture().get();
                        logger.info("agent {} FAIL test {}", test, agent.getName()  + ":" + threadNumber);
                    }
                    threadNumber += 1;
                    Thread.sleep(DELAY_BETWEEN_TESTS_MS);
                }
            }
        }
    }

    private static List<Test> takeTests(NewmanClient newmanClient, String agentName, String jobId) throws ExecutionException, InterruptedException {
        List<Test> res = new ArrayList<>(AGENT_THREADS);
        while (res.size() < AGENT_THREADS) {
            Test test = newmanClient.getReadyTest(agentName, jobId).toCompletableFuture().get();
            if (test == null) {
                return res;
            } else {
                res.add(test);
            }
        }
        return res;
    }
}
