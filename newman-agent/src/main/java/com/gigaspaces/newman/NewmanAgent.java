package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 @author Boris
 @since 1.0
 Agent that executes the tests.
 */
public class NewmanAgent {

    private static final Logger logger = LoggerFactory.getLogger(NewmanAgent.class);
    AtomicInteger localId = new AtomicInteger(1); //TODO rm this var
    private final NewmanAgentConfig config;
    private final ThreadPoolExecutor workers;
    private final String name;
    private final NewmanClient client;
    private volatile boolean active = true;

    public static void main(String[] args) {

        NewmanAgent agent = new NewmanAgent();
        try {
            agent.start();
        } catch (InterruptedException e) {
            logger.info("Agent was interrupted");
        }
        catch (Exception e) {
            logger.error("Agent was stopped unexpectedly", e);
        }
        finally {
            agent.close();
        }
        logger.info("Agent is stopping...");
    }

    public NewmanAgent(){
        this.config = new NewmanAgentConfig("newman_agent.properties");
        this.name = "newman-agent-" + UUID.randomUUID().toString();
        this.workers = new ThreadPoolExecutor(config.getNumOfWorkers(), config.getNumOfWorkers(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        logger.info("Agent is initializing...");
        this.client = NewmanClient.create("root", "root");
    }

    private void start() throws InterruptedException {
        while (active){
            Job job = waitForJob();
            final JobExecutor jobExecutor = new JobExecutor(job, config.getNewmanHome());
            boolean setupFinished = jobExecutor.setup();
            if (!setupFinished){
                logger.error("Setup of job {} failed, will wait for a new job", job.getId());
                jobExecutor.teardown();
                //TODO inform the server that agent is not working on this job
                continue;
            }

            // Submit workers:
            for (int i =0; i < config.getNumOfWorkers(); i++) {
                final int id = i;
                workers.submit(() -> {
                    logger.info("Starting worker #{} for job {}", id, jobExecutor.getJob().getId());
                    Test test;
                    while ((test = findTest(jobExecutor.getJob())) != null) {
                        TestResult testResult = jobExecutor.run(test);
                        reportTest(testResult);
                    }
                    logger.info("Finished Worker #{} for job {}", id, jobExecutor.getJob().getId());
                });
            }

            // Wait for all workers to complete:
            while (workers.getActiveCount() != 0){
                logger.info("Waiting for all workers to complete ({} active workers)...", workers.getActiveCount());
                Thread.sleep(config.getActiveWorkersPollInterval());
            }

            jobExecutor.teardown();
        }
    }

    private void close(){
        client.close();
        active = false;
    }

    private Job waitForJob() throws InterruptedException {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setHost(config.getHostName());

        while (true){
            try {
                //TODO rm comment
                Job job = createMockJob()/*client.subscribe(agent).toCompletableFuture().get()*/;
                if (job != null)
                    return job;
                logger.info("Agent did not find a job to run, will try again in {} ms", config.getJobPollInterval());
            } catch (ExecutionException e) {
                logger.warn("Agent failed while polling for a job (retry in {} ms): " + e, config.getJobPollInterval());
            }
            Thread.sleep(config.getJobPollInterval());
        }
    }

    private Test findTest(Job job)  {
        try {
            // TODO rm comment and the mock if
            if (localId.get() % 6 == 0 ){
                return null;
            }
            return createMockTest(job.getId(), name)/*client.getReadyTest(name, job.getId()).toCompletableFuture().get()*/;
        } catch (InterruptedException e) {
            logger.info("Worker #{} was interrupted while waiting for test");
            return null;
        } catch (ExecutionException e) {
            logger.error("Worker #{} failed while waiting for test: " + e);
            return null;
        }
    }

    private void reportTest(TestResult testResult) {
        logger.info("Test #" + testResult.getTestId() + (testResult.isPassed() ? " passed" : " failed"));
        // TODO: Report test;
        // TODO: Upload test logs;
    }

    //TODO rm this method
    public Test createMockTest(String jobId, String agentId) throws ExecutionException,InterruptedException {
        Test t = new Test();

        t.setJobId(jobId);
        t.setId(UUID.randomUUID().toString());
        t.setStatus(Test.Status.RUNNING);
        t.setAssignedAgent(agentId);
        t.setTimeout(1000 * 60);
        t.setTestType("junit");
        t.setLocalId(localId.getAndIncrement());
        Collection<String> args = new ArrayList<>();
        String testName = "com.gigaspaces.test.newman.NewmanBasicMockTest";
        String methodName = "testMock";
        args.add(testName);
        args.add(methodName);
        t.setArguments(args);

        return t;
    }

    //TODO rm this method
    public Job createMockJob() throws ExecutionException {
        Job j = new Job();

        URI artifactsURI = URI.create("http://tarzan/users/boris/newman/newman-artifacts.zip");
        URI buildURI = URI.create("http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13500-203/xap-premium/1.5/gigaspaces-xap-premium-10.2.0-m1-b13500-203.zip");
        URI testsURI = URI.create("http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13500-203/testsuite-1.5.zip");
        Collection<URI> collection = new ArrayList<>();
        collection.add(artifactsURI);
        collection.add(buildURI);
        collection.add(testsURI);
        j.setResources(collection);

        j.setId(UUID.randomUUID().toString());
        j.setSubmittedBy("mock");
        j.setState(State.READY);
        return j;
    }
}
