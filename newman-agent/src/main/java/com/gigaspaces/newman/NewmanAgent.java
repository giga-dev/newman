package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    private String name;
    private NewmanClient client;
    private volatile boolean active = true;
    private final Timer timer = new Timer(true);
    private final String agentNameFile = append(Paths.get("."), "newman_agent_name.txt").toAbsolutePath().normalize().toString();

    public static void main(String[] args) {
        NewmanAgent agent = new NewmanAgent();
        agent.initialize();
        try {
            agent.start();
        } catch (Exception e) {
            agent.deactivateAgent(e, "Agent was stopped unexpectedly");
        } finally {
            agent.close();
        }
    }

    public NewmanAgent(){
        this.config = new NewmanAgentConfig();
        this.workers = new ThreadPoolExecutor(config.getNumOfWorkers(), config.getNumOfWorkers(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        this.active = true;
    }

    private void initialize() {
        logger.info("Agent is initializing...");
        logger.info("Agent home dir is {}, please make sure no other agent is using this folder", config.getNewmanHome());
        try {
            this.client = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                    config.getNewmanServerRestUser(), config.getNewmanServerRestPw());
            //try to connect to fail fast when server is down
            client.getJobs().toCompletableFuture().get();
        } catch (Exception e) {
            deactivateAgent(e, "Rest client failed to initialize, exiting ...");
        }
        try {
            FileUtils.createFolder(append(config.getNewmanHome(), "logs"));
        } catch (IOException e) {
            deactivateAgent(e, "Failed to create logs folder in " +append(config.getNewmanHome(), "logs"));
        }
        setFinalAgentName();

        attachShutDownHook();
    }

    private void attachShutDownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.warn("Shutting down...");
                close();
                try {
                    FileUtils.delete(new File(config.getNewmanHome()).toPath());
                }
                catch (IOException e) {
                    logger.warn("failed to clean newman agent dir {} retrying ...", config.getNewmanHome());
                    try {
                        // retry if race condition occurred and worker threads already deleted files while shutting down
                        Thread.sleep(1000 * 5);
                        FileUtils.delete(new File(config.getNewmanHome()).toPath());
                    }
                    catch (Exception e2){
                        logger.warn("failed to clean newman agent dir {}", config.getNewmanHome());
                    }
                }
            }
        });
    }

    private void setFinalAgentName() {
        if (!config.isPersistentName()){
            this.name = "newman-agent-" + UUID.randomUUID().toString();
        }
        // persist name in working directory
        else {
            logger.info("Agent name will be stored in {}, please make sure no other agent is running on the same working directory", agentNameFile);
            try {
                final File nameFile = new File(agentNameFile);
                if (!nameFile.exists()) {
                    this.name = "newman-agent-" + UUID.randomUUID().toString();
                    FileUtils.writeLineToFile(agentNameFile, name);
                }
                // read from file if exists after recovery
                else {
                    this.name = FileUtils.readTextFile(nameFile.toPath());
                    logger.info("read agent name {} from file {} after agent recovery", name, agentNameFile);
                }
            } catch (Exception e) {
                deactivateAgent(e, "failed to persist agent name: " + name + " to a file: " + agentNameFile);
            }
        }
    }

    private void start() {
        while (isActive()){
            Job job = waitForJob();
            // ping server during job setup and execution
            final KeepAliveTask keepAliveTask = startKeepAliveTask(job.getId());
            final JobExecutor jobExecutor = new JobExecutor(job, config.getNewmanHome());
            boolean setupFinished = jobExecutor.setup();
            if (!setupFinished){
                logger.error("Setup of job {} failed, will wait for a new job", job.getId());
                jobExecutor.teardown();
                //inform the server that agent is not working on this job
                Agent agent;
                try {
                    agent = client.getAgent(name).toCompletableFuture().get();
                    client.unsubscribe(agent).toCompletableFuture().get();
                } catch (Exception e) {
                    logger.warn("Failed to unsubscribe agent {} due to failure in job setup of job {}", name, job.getId());
                }
                try {
                    Thread.sleep(config.getJobPollInterval());
                } catch (InterruptedException ignored) {}
                continue;
            }

            // Submit workers:
            List<Future<?>> workersTasks = new ArrayList<>();
            for (int i =0; i < config.getNumOfWorkers(); i++) {
                final int id = i;
                Future<?> worker = workers.submit(() -> {
                    logger.info("Starting worker #{} for job {}", id, jobExecutor.getJob().getId());
                    Test test;
                    while ((test = findTest(jobExecutor.getJob())) != null) {
                        Test testResult = jobExecutor.run(test);
                        reportTest(testResult);
                    }
                    logger.info("Finished Worker #{} for job {}", id, jobExecutor.getJob().getId());
                });
                workersTasks.add(worker);
            }
            for (Future<?> worker : workersTasks){
                logger.info("Waiting for all workers to complete...");
                try {
                    worker.get();
                }
                catch (Exception e){
                    logger.warn("worker exited with exception", e);
                }
            }
            keepAliveTask.cancel();
            jobExecutor.teardown();
        }
    }


    private KeepAliveTask startKeepAliveTask(String jobId) {
        final KeepAliveTask task = new KeepAliveTask(jobId);
        timer.scheduleAtFixedRate(task, 10 , config.getPingInterval());
        return task;
    }

    public boolean isActive() {
        return active;
    }

    private synchronized void deactivateAgent(Exception e, String s) {
        logger.error(s, e);
        active = false;
    }

    private void close(){
        if (isActive()) {
            active = false;
            logger.info("Closing newman agent {}", name);
            shutDownWorkers();
            if (client != null) {
                try {
                    client.freeAgent(name).toCompletableFuture().get();
                } catch (Exception e) {
                    logger.error("failed to free the agent" + name, e);
                }
                client.close();
            }
        }
        if (timer != null){
            timer.cancel();
            timer.purge();
        }
    }

    private void shutDownWorkers() {
        workers.shutdown();
        long workerExecutorShutdownTime = 1;
        try {
            if (!workers.awaitTermination(workerExecutorShutdownTime, TimeUnit.MINUTES)) {
                logger.warn("workers executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = workers.shutdownNow();
                logger.warn("workers executor was abruptly shut down. {} tasks will not be executed.", droppedTasks.size());
            }
        } catch (InterruptedException e) {
            logger.warn("workers executor did not terminate in the specified time.");
            List<Runnable> droppedTasks = workers.shutdownNow();
            logger.warn("workers executor was abruptly shut down. {} tasks will not be executed.", droppedTasks.size());
        }
    }

    private Job waitForJob() {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setHost(config.getHostName());

        while (true){
            try {
                Job job = client.subscribe(agent).toCompletableFuture().get();
                if (job != null)
                    return job;
                logger.info("Agent[{}] did not find a job to run, will try again in {} ms", agent, config.getJobPollInterval());
            } catch (ExecutionException e) {
                logger.warn("Agent failed while polling newman-server at {} for a job (retry in {} ms): " + e, config.getNewmanServerHost(), config.getJobPollInterval());
            }
            catch (InterruptedException e) {
                logger.warn("Agent got InterruptedException while polling for a job (retry in {} ms): " + e, config.getJobPollInterval());
            }
            try {
                Thread.sleep(config.getJobPollInterval());
            } catch (InterruptedException ignored) {}
        }
    }

    private Test findTest(Job job) {
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
                client.uploadLog(testResult.getId(), (File) log);
            }
            // removes local test log dir once the logs are published to the server
            //delete(testLogsFolder);
        } catch (InterruptedException e) {
            logger.info("Worker was interrupted while updating test result: {}", testResult);
        } catch (ExecutionException e) {
            logger.warn("Worker failed to update test result: {} caught: {}", testResult, e);
        } catch (IOException e) {
            logger.warn("Worker failed to unzip logs test result caught: {}", e);
        }
    }

    private class KeepAliveTask extends TimerTask{

        private String jobId;

        public KeepAliveTask(String jobId){

            this.jobId = jobId;
        }

        @Override
        public void run() {
            if (isActive()){
                try {
                    //logger.info("sending ping to server, jobid= {}", jobId);
                    client.ping(name, jobId).toCompletableFuture().get(10, TimeUnit.SECONDS);
                } catch (Exception ignored) {}
            }
        }
    }
}


