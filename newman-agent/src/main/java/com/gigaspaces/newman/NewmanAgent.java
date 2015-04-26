package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final NewmanAgentConfig config;
    private final ThreadPoolExecutor workers;
    private final String name;
    private final NewmanClient client;
    private volatile boolean active;

    public static void main(String[] args) {

        NewmanAgent agent = new NewmanAgent();
        try {
            agent.start();
        } catch (InterruptedException e) {
            logger.info("Agent was interrupted");
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
            jobExecutor.setup();

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
                Job job = client.subscribe(agent).toCompletableFuture().get();
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
            return client.getReadyTest(name, job.getId()).toCompletableFuture().get();
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
}
