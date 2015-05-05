package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.*;

import static com.gigaspaces.newman.utils.FileUtils.append;
import static com.gigaspaces.newman.utils.FileUtils.unzip;

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

    private void start() throws InterruptedException, ExecutionException, IOException {
        FileUtils.createFolder(append(config.getNewmanHome(), "logs"));
        while (active){
            Job job = waitForJob();
            final JobExecutor jobExecutor = new JobExecutor(job, config.getNewmanHome());
            boolean setupFinished = jobExecutor.setup();
            if (!setupFinished){
                logger.error("Setup of job {} failed, will wait for a new job", job.getId());
                jobExecutor.teardown();
                //inform the server that agent is not working on this job
                Agent agent = client.getAgent(name).toCompletableFuture().get();
                client.unsubscribe(agent).toCompletableFuture().get();
                continue;
            }

            // Submit workers:
            for (int i =0; i < config.getNumOfWorkers(); i++) {
                final int id = i;
                workers.submit(() -> {
                    logger.info("Starting worker #{} for job {}", id, jobExecutor.getJob().getId());
                    Test test;
                    while ((test = findTest(jobExecutor.getJob())) != null) {
                        Test testResult = jobExecutor.run(test);
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
            logger.info("Worker was interrupted while waiting for test");
            return null;
        } catch (ExecutionException e) {
            logger.error("Worker failed while waiting for test: " + e);
            return null;
        }
    }

    private void reportTest(Test testResult) {
        logger.info("Reporting Test #{} status: {}",testResult.getId(),testResult.getStatus());

        try {
            //update test removes Agent assignment
            client.finishTest(testResult).toCompletableFuture().get();
            Path logs = append(config.getNewmanHome(), "logs");
            Path testLogsFile = append(logs, "output-" + testResult.getId() + ".zip");
            Path testLogsFolder = append(logs, "output-" + testResult.getId());
            unzip(testLogsFile, testLogsFolder);
            Collection logFiles = FileUtils.listFilesInFolder(testLogsFolder.toFile());
            for (Object log : logFiles){
                client.uploadLog(testResult.getId(), (File)log);
            }
        } catch (InterruptedException e) {
            logger.info("Worker was interrupted while updating test result: {}", testResult);
        } catch (ExecutionException e) {
            logger.warn("Worker failed to update test result: {} caught: {}", testResult, e);
        } catch (IOException e) {
            logger.warn("Worker failed to unzip logs test result caught: {}", e);
        }
    }
}
