package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.criteria.CriteriaBuilder;
import com.gigaspaces.newman.beans.criteria.PatternCriteria;
import com.gigaspaces.newman.beans.criteria.TestCriteria;
import com.gigaspaces.newman.utils.EnvUtils;
import com.gigaspaces.newman.utils.FileUtils;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
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

import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.exclude;
import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.include;
import static com.gigaspaces.newman.utils.StringUtils.notEmpty;

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

    public static final String NEWMAN_CRITERIA_TEST_TYPE = "NEWMAN_CRITERIA_TEST_TYPE";
    public static final String NEWMAN_CRITERIA_INCLUDE_LIST = "NEWMAN_CRITERIA_INCLUDE_LIST";
    public static final String NEWMAN_CRITERIA_EXCLUDE_LIST = "NEWMAN_CRITERIA_EXCLUDE_LIST";
    public static final String NEWMAN_CRITERIA_PERMUTATION_URI = "NEWMAN_CRITERIA_PERMUTATION_URI";

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
            CompletableFuture<Suite> suiteCompletableFuture = newmanClient.getSuite("625e9faf41b6b5b45f4b48f0").toCompletableFuture();
            Suite suit =suiteCompletableFuture.get();
            System.out.println(suit);
//            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//            System.out.println(gson.toJson(suit));
            JobRequest jobRequest = new JobRequest();
            jobRequest.setBuildId("5cf3f8ec4cedfd000c4ba171");
            jobRequest.setSuiteId("625e9faf41b6b5b45f4b48f0");
            jobRequest.setConfigId("5bf160bb1f31eb789fc0fb78");

            Job job = newmanClient.createJob(jobRequest).toCompletableFuture().get();
            List<Test> tests =new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Test test = new Test();
                test.setStatus(Test.Status.FAIL);
                test.setJobId(job.getId());
                test.setName("test_" + i);
                test.setArguments(Arrays.asList(Test.class.getName()/*, "arg1", "arg2" */));
                tests.add(test);
                 logger.info("added test {}", test);
            }
            newmanClient.createTests(tests,"count").toCompletableFuture().get();


            Suite suite = newmanClient.createSuiteFromFailingTests(job.getId(),"sapir_moran").toCompletableFuture().get();



        } catch (Exception e) {
            logger.error(e.toString(), e);
        } finally {
            newmanClient.close();
        }
    }

    public static void main__2(String[] args) throws KeyManagementException, NoSuchAlgorithmException {

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

    private static void createAndRunJob(NewmanClient newmanClient, Build build) throws Exception {

        for (int s = 0; s < NUMBER_OF_SUITES_PER_BUILD; s++) {

            Suite mySuite = new Suite();
            mySuite.setName("Suite-" + UUID.randomUUID().toString().substring(0, 3));
            mySuite.setCustomVariables( "customVarKey1=val1,customVarKey2=val2" );
            mySuite.setCriteria( getNewmanSuiteCriteria() );
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
                test = newmanClient.uploadTestLog( jobId, test.getId(), new File("mongo.txt")).toCompletableFuture().get();
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

    public static Criteria getNewmanSuiteCriteria() throws Exception {
        List<Criteria> criterias = new ArrayList<>();

        String testType = EnvUtils.getEnvironment(NEWMAN_CRITERIA_TEST_TYPE, false /*required*/, logger);
        if (notEmpty(testType)) {
            criterias.add(TestCriteria.createCriteriaByTestType(testType));
        }

        String includeList = EnvUtils.getEnvironment(NEWMAN_CRITERIA_INCLUDE_LIST, false /*required*/, logger);
        if (notEmpty(includeList)) {
            criterias.add(include(parseCommaDelimitedList(includeList)));
        }

        String excludeList = EnvUtils.getEnvironment(NEWMAN_CRITERIA_EXCLUDE_LIST, false /*required*/, logger);
        if (notEmpty(excludeList)) {
            criterias.add(exclude(parseCommaDelimitedList(excludeList)));
        }

        String permutationURI = EnvUtils.getEnvironment(NEWMAN_CRITERIA_PERMUTATION_URI, false /*required*/, logger);
        if (notEmpty(permutationURI)) {
            criterias.add(include(getTestCriteriasFromPermutationURI(permutationURI)));
        }
        return CriteriaBuilder.join(criterias.toArray(new Criteria[criterias.size()]));
    }

    private static Criteria[] getTestCriteriasFromPermutationURI(String permutationURI) throws Exception {
        List<Criteria> criterias = new ArrayList<>();
        InputStream is = null;
        try {
            is = URI.create(permutationURI).toURL().openStream();
            List<String> permutations = FileUtils.readTextFileLines(is);
            for (String permutation : permutations) {
                if (permutation.length() <= 1)
                    continue;
                if (permutation.trim().charAt(0) == '#')
                    continue;
                TestCriteria criteria = TestCriteria.createCriteriaByTestArgs(permutation.split(" "));
                criterias.add(criteria);
            }
        }
        finally {
            if (is != null) {
                is.close();
            }
        }
        return criterias.toArray(new Criteria[criterias.size()]);
    }

    private static Criteria[] parseCommaDelimitedList(String list) {
        if (list.length() == 0) return new Criteria[0];

        List<Criteria> listOfCriterias = new ArrayList<>();
        String[] split = list.trim().split(",");
        for (String criteria : split) {
            criteria = criteria.trim();
            if (notEmpty(criteria)) {
                listOfCriterias.add(PatternCriteria.containsCriteria(criteria));
            }
        }
        return listOfCriterias.toArray(new Criteria[listOfCriterias.size()]);
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
