package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.config.AgentConfig;
import com.gigaspaces.newman.config.SystemProperties;
import com.gigaspaces.newman.luncher.TestLauncher;
import com.gigaspaces.newman.util.FileUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 @author Boris
 @since 1.0
 Agent that executes the tests.
 */
public class NewmanAgent {

    private static final Logger logger = LoggerFactory.getLogger(NewmanAgent.class);
    private static final int workersPoolSize = Integer.getInteger(SystemProperties.WORKERS_POOL_SIZE, SystemProperties.DEFAULT_WORKERS_POOL_SIZE);
    private static volatile boolean stop;
    private ThreadPoolExecutor workersPool;
    private NewmanClient client;
    private AgentConfig config;
    private final String name = "newman-agent-" + UUID.randomUUID().toString();
    private Job currentJob;

    public NewmanAgent(AgentConfig config){
        this.config = config;
    }

    private void initialize(String[] args) {
        logger.info("Agent is initializing...");
        // TODO use args or whatever to initialize client
        client = NewmanClient.create("root", "root");
    }

    public String getName() {
        return name;
    }

    public AgentConfig getConfig() {
        return config;
    }

    private void initializeWorkers() {
        workersPool = new ThreadPoolExecutor(workersPoolSize, workersPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    private void triggerWorkers() {
        for (int i =0; i<workersPoolSize; i++){
            workersPool.submit(new AgentWorkerTask(this, i));
        }
    }

    /**
     * Subscribes the agent to the server and waits for a job, when the job arrives installs the build and tests in order to be able to execute the tests
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void prepareJob() throws Exception {

        Job job = subscribe();

        while (job == null){
            try {
                Integer sleepInterval = Integer.getInteger(SystemProperties.FETCH_JOB_INTERVAL, SystemProperties.DEFAULT_FETCH_JOB_INTERVAL);
                logger.info("Agent could not find a job to run, will try again in {} mills", sleepInterval);
                Thread.sleep(sleepInterval);
            }
            catch (InterruptedException e) {}

            job = subscribe();
        }

        //TODO refactor into installResources to be generic or move to scripts
        installBuild(job.getBuild());

        installTests(job.getTestURI());

        currentJob = job;
    }

    private void installTests(URI testURI) throws Exception {
        logger.info("installing tests from URI: {}", testURI);
        URI check = new URI("http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13500-106/testsuite-1.5.zip");
        //TODO rm the comment instead of check
        FileUtilities.download(logger, check /*testURI*/, config.getTestsLocation(), "tests.zip");
        String testZipLocation = config.getTestsLocation() + AgentConfig.fileSeperator + "tests.zip";
        FileUtilities.unzip(logger, testZipLocation, config.getTestsLocation());
        FileUtilities.rmFile(testZipLocation);
    }

    private void installBuild(Build currentBuild) throws URISyntaxException {
        logger.info("installing build {}", currentBuild.toString());
        URI check = new URI("http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13500-106/xap-premium/1.5/gigaspaces-xap-premium-10.2.0-m1-b13500-106.zip");
        //TODO rm the comment instead of check
        FileUtilities.download(logger, check/*currentBuild.getUri()*/, config.getBuildLocation(), "build.zip");
        String buildZipLocation = config.getBuildLocation() + AgentConfig.fileSeperator + "build.zip";
        FileUtilities.unzip(logger, buildZipLocation, config.getBuildLocation());
        FileUtilities.rmFile(buildZipLocation);
    }

    private void close(){
        client.close();
        stop = true;
    }

    private void ping(String testId) {
        client.ping(name, currentJob.getId(), testId);
    }

    private Job subscribe() throws ExecutionException, InterruptedException {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setHost(config.getHostName());
        return client.subscribe(agent).toCompletableFuture().get();
    }

    private void tearDownJob() {
        logger.info("Agent will uninstall XAP and remove tests folders");
        FileUtilities.rmFile(config.getBuildLocation());
        FileUtilities.rmFile(config.getTestsLocation());
        //TODO maybe signal job is done somehow?
    }

    private void waitForJobTermination() throws InterruptedException {
        int activeCount = workersPool.getActiveCount();
        while (activeCount != 0){
            logger.info("waiting for all workers to finish executing tests, {} workers are still working", activeCount);
            int interval = Integer.getInteger(SystemProperties.WORKERS_POLLING_INTERVAL, SystemProperties.DEFAULT_WORKERS_POLLING_INTERVAL);
            Thread.sleep(interval);
            activeCount = workersPool.getActiveCount();
        }
    }

    private void work() throws Exception {

        while (!stop){

            initializeWorkers();

            //fetch job and install requirements(xap and tests)
            prepareJob();

            triggerWorkers();

            waitForJobTermination();

            tearDownJob();
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        AgentConfig config = new AgentConfig("newman_agent.properties");
        NewmanAgent agent = new NewmanAgent(config);
        try {
            agent.initialize(args);
            agent.work();
        }
        catch (Exception e){
            logger.error("Agent suddenly stopped", e);
        }
        finally {
            agent.close();
        }

        logger.info("Agent is stopping...");
    }



    private class AgentWorkerTask implements Runnable {

        private NewmanAgent agent;
        private int id;
        private boolean shouldWork = true;
        private TestLauncher launcher = new TestLauncher();
        private String workingDirectory;

        public AgentWorkerTask(NewmanAgent agent, int id) {
            this.agent = agent;
            this.id = id;
            workingDirectory = agent.getConfig().getNewmanLocation() + AgentConfig.fileSeperator + "worker" + id;
        }

        @Override
        public void run() {
            while (shouldWork) {
                try {
                    FileUtilities.makeDir(workingDirectory);
                    //TODO copy scripts to dir
                    //take test and mark it as taken
                    Test toRun = getTestToRun(agent.getName());
                    if (toRun != null) {
                        //TODO execute test in working directory according to the id and post the result
                        logger.info("executing test: {}", toRun);
                        agent.ping(toRun.getId());
                        String mockTestName = "com.gigaspaces.concurrent.AbstractConcurrentTest";
                        //TODO add timeout to test
                        launcher.execute(/*toRun.getName()*/ mockTestName, workingDirectory, 60 * 1000 /*toRun.getTimeout()*/);
                        //TODO handle logs/timeout/fail/success
                    } else {
                        logger.info("no test returned, worker is exiting");
                        shouldWork = false;
                    }
                } catch (Exception e) {
                    //TODO handle http 500 when no test to run, should return null from server
                    logger.info("no test returned, worker is exiting");
                    shouldWork = false;
                }
            }
            logger.info("Worker[{}] finished his test execution", id);
        }

        private Test getTestToRun(String agentName) throws ExecutionException, InterruptedException {
            return client.getReadyTest(agentName, currentJob.getId()).toCompletableFuture().get();
        }
    }
}
