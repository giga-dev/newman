package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.utils.FileUtils;
import com.gigaspaces.newman.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static com.gigaspaces.newman.utils.FileUtils.append;

/**
 * @author Boris
 * @since 1.0
 * Agent that executes the tests.
 */
public class NewmanAgent {

    private static final Logger logger = LoggerFactory.getLogger(NewmanAgent.class);
    private final NewmanAgentConfig config;
    private final ThreadPoolExecutor workers;
    private String name;
    private volatile NewmanClient client;
    private volatile boolean active = true;
    private final Timer timer = new Timer(true);
//    private static long lastTimeFailedToConnectToServer;
//    private final long minutesToPassBeforeExit = 15;

    public static void main(String[] args) {
//        updateCurrentTime();
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

    public NewmanAgent() {
        this.config = new NewmanAgentConfig();
        this.workers = new ThreadPoolExecutor(config.getNumOfWorkers(), config.getNumOfWorkers(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        this.active = true;
    }

    private void initialize() {
        logger.info("Agent is initializing...");
        logger.info("Agent home dir is {}, please make sure no other agent is using this folder", config.getNewmanHome());
        while (true) {
            try {
                this.client = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                        config.getNewmanServerRestUser(), config.getNewmanServerRestPw());
                //try to connect to fail fast when server is down
                client.getJobs().toCompletableFuture().get();
                FileUtils.createFolder(append(config.getNewmanHome(), "logs"));
                break;
            } catch (Exception e) {
                final int timeToWait = 20 * 1000;
                logger.warn("Failed to initialize newman agent in ["+config.getHostName()+"] retrying in "+timeToWait/1000+" seconds", e);
                try {
                    Thread.sleep(timeToWait);
                } catch (InterruptedException ignored) {}
            }
        }
        setFinalAgentName();

        attachShutDownHook();
    }

    private void attachShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.warn("Shutting down...");
                close();
                try {
                    FileUtils.delete(new File(config.getNewmanHome()).toPath());
                } catch (IOException e) {
                    logger.warn("failed to clean newman agent dir {} retrying ...", config.getNewmanHome());
                    try {
                        // retry if race condition occurred and worker threads already deleted files while shutting down
                        Thread.sleep(1000 * 5);
                        FileUtils.delete(new File(config.getNewmanHome()).toPath());
                    } catch (Exception e2) {
                        logger.warn("failed to clean newman agent dir {}", config.getNewmanHome());
                    }
                }
            }
        });
    }

    private void setFinalAgentName() {
        this.name = "newman-agent-" + UUID.randomUUID().toString();
    }

    private void start() {
        NewmanClient c = getClient();
        KeepAliveTask keepAliveTask = null;
        while (isActive()) {
            Job job = waitForJob();
            // ping server during job setup and execution
            keepAliveTask = startKeepAliveTask(job.getId(), keepAliveTask);
            final JobExecutor jobExecutor = new JobExecutor(job, config.getNewmanHome());
            boolean setupFinished = jobExecutor.setup();
            if (!setupFinished) {
                logger.error("Setup of job {} failed, will wait for a new job", job.getId());
                jobExecutor.teardown();
                //inform the server that agent is not working on this job
                Agent agent;
                try {
                    agent = c.getAgent(name).toCompletableFuture().get();
                    c.unsubscribe(agent).toCompletableFuture().get();
                }
                catch (IllegalStateException e){
                    c = getClient();
                }
                catch (Exception e) {
                    logger.warn("Failed to unsubscribe agent {} due to failure in job setup of job {}", name, job.getId());
                }
                try {
                    Thread.sleep(config.getJobPollInterval());
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            // Submit workers:
            List<Future<?>> workersTasks = new ArrayList<>();
            for (int i = 0; i < calculateNumberOfWorkers(job); i++) {
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
            for (Future<?> worker : workersTasks) {
                logger.info("Waiting for all workers to complete...");
                try {
                    worker.get();
                } catch (Exception e) {
                    logger.warn("worker exited with exception", e);
                }
            }
            keepAliveTask.cancel();
            jobExecutor.teardown();
        }
    }

    private int calculateNumberOfWorkers(Job job) {
        if (job.getSuite() == null || job.getSuite().getCustomVariables() == null) {
            logger.info("Configured number of workers: " + config.getNumOfWorkers());
            return config.getNumOfWorkers();
        }
        String limit = Suite.parseCustomVariables(job.getSuite().getCustomVariables()).getOrDefault(Suite.THREADS_LIMIT, Integer.toString(config.getNumOfWorkers()));
        int min = Math.min(config.getNumOfWorkers(), Integer.parseInt(limit));
        logger.info("Calculated number of workers: " + min + " (config: " + config.getNumOfWorkers() + ", limit: " + limit+")s");
        return min;
    }


    private KeepAliveTask startKeepAliveTask(String jobId, final KeepAliveTask prevKeepAliveTask) {
        if(prevKeepAliveTask != null){
            prevKeepAliveTask.cancel();
        }

        final KeepAliveTask task = new KeepAliveTask(jobId);
        timer.scheduleAtFixedRate(task, 10, config.getPingInterval());
        return task;
    }

    public boolean isActive() {
        return active;
    }

    private void deactivateAgent(Exception e, String s) {
        logger.error(s, e);
        active = false;
    }

    private void close() {
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
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
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
        agent.setHostAddress(config.getHostAddress());
        agent.setPid(ProcessUtils.getProcessId("unknownPID"));
        String capabilities = config.getCapabilities();
        // TODO note - if set is empty, mongodb will NOT write that filed to DB
        agent.setCapabilities(CapabilitiesAndRequirements.parse(capabilities));
        int logRepeats = 3;
        NewmanClient c = getClient();
        while (true) {
            try {
                Job job = c.subscribe(agent).toCompletableFuture().get();
                if (job != null)
                    return job;

                if (--logRepeats >= 0) {
                    if (logRepeats == 0) {
                        logger.info("Agent[{}] will quietly try to find a job to run every {} ms", agent, config.getJobPollInterval());
                    } else {
                        logger.info("Agent[{}] did not find a job to run, will try again in {} ms", agent, config.getJobPollInterval());
                    }
                }
            }
            catch (IllegalStateException e) {
                logger.warn("Agent failed while polling newman-server at {} for a job (retry in {} ms): " + e, config.getNewmanServerHost(), config.getJobPollInterval());
                c = getClient();
            }
            catch (Exception e) {
                logger.warn("Agent failed while polling newman-server at {} for a job (retry in {} ms): " + e, config.getNewmanServerHost(), config.getJobPollInterval());
                c = onClientFailure(c);
            }
            try {
                Thread.sleep(config.getJobPollInterval());
            } catch (InterruptedException ignored) {
            }
        }
    }

    private synchronized NewmanClient getClient() {
        return client;
    }

//    private static void updateCurrentTime(){
//        lastTimeFailedToConnectToServer = System.currentTimeMillis();
//    }

    private synchronized NewmanClient onClientFailure(NewmanClient localClient) {
//        if(lastTimeFailedToConnectToServer == -1){
//            updateCurrentTime();
//        }
        if (localClient != getClient()) { // in case other thread already restarted the client
            return getClient();
        }

        if (client != null) {
            client.close();
        }
        while (true) {
//            long currentTime =  System.currentTimeMillis();
//            long timePassedSineStartToFail = currentTime - lastTimeFailedToConnectToServer;
//            long timePassedSineStartToFailMinutes = TimeUnit.MILLISECONDS.toMinutes(timePassedSineStartToFail);
//            if(timePassedSineStartToFailMinutes > minutesToPassBeforeExit){
//                throw new IllegalArgumentException("agent did not connect for the last " + timePassedSineStartToFailMinutes + ", resting agent...");
//            }
            try {
                logger.info("handling client failure, reconnecting to newman server ip: {}, port: {}", config.getNewmanServerHost(), config.getNewmanServerPort());
                client = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                        config.getNewmanServerRestUser(), config.getNewmanServerRestPw());
                //try to connect to fail fast when server is down
                client.ping(name, "");
                break;
            } catch (Exception e) {
                logger.warn("failure while recreation of newman client, will retry...", e);
            }
        }
        return getClient();
    }

    private Test findTest(Job job) {
        try {
            return getClient().getReadyTest(name, job.getId()).toCompletableFuture().get();
        } catch (InterruptedException e) {
            logger.info("Worker was interrupted while waiting for test");
            return null;
        } catch (ExecutionException e) {
            logger.error("Worker failed while waiting for test: " + e);
            return null;
        }
    }

    private void reportTest(Test testResult) {
        NewmanClient c = getClient();
        logger.info("Reporting Test #{} JobId #{} Status: {}", testResult.getId(), testResult.getJobId(), testResult.getStatus());
        while (true) {
            try {
                //update test removes Agent assignment
                c.finishTest(testResult).toCompletableFuture().get();
                Path logs = append(config.getNewmanHome(), "logs");
                Path testLogsFile = append(logs, "output-" + testResult.getId() + ".zip");
                c.uploadLog(testResult.getJobId(), testResult.getId(), testLogsFile.toFile());
                break;
            }
            catch (IllegalStateException e){ // client was closed
                c = getClient();
            }
            catch (Exception e) {
                logger.warn("Worker failed to update test result: {} caught: {}", testResult, e);
                c = onClientFailure(c);
            }
        }
    }

    private class KeepAliveTask extends TimerTask {

        private String jobId;

        public KeepAliveTask(String jobId) {

            this.jobId = jobId;
        }

        @Override
        public void run() {
            if (isActive()) {
                NewmanClient c = getClient();
                try {
                    //logger.info("sending ping to server, jobid= {}", jobId);
                    c.ping(name, jobId).toCompletableFuture().get(10, TimeUnit.SECONDS);
                }
                catch (Exception ignored) {
                }
            }
        }
    }
}


