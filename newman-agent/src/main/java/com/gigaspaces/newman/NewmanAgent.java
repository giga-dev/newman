package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.utils.FileUtils;
import com.gigaspaces.newman.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
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
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int MILLISECONDS_IN_SECOND = 1000;
    private volatile boolean workerShouldStop = false;
    private volatile long lastPriorityCheckedTime ;

    public static void main(String[] args) throws Exception {
        NewmanAgent agent = new NewmanAgent();
        if (args.length > 0 && args[0].trim().equalsIgnoreCase("setup")){
            createJobSetupEnv(args, agent);
        }
        agent.initialize(true);
        try {
            agent.start();
        } catch (Exception e) {
            logger.error("Agent was stopped unexpectedly", e);
        } finally {
            agent.close();
            System.exit(1);
        }
    }

    private static void createJobSetupEnv(String[] args, NewmanAgent agent) throws ExecutionException, InterruptedException, IOException, TimeoutException {
        logger.info("performing job setup env");
        agent.initialize(false);
        if (args.length != 4) {
            logger.error("invalid num of args");
            logger.error("Usage: java -jar newman-agent-1.0.jar <setup> <suiteId> <buildId> <CONFIG_ID> ... [list-of-regular-system-properties]");
            System.exit(1);
        }
        else {
            String suiteId = args[1];
            String buildId = args[2];
            String configId = args[3];
            agent.prepareJobSetupEnv(suiteId, buildId,configId);
            System.exit(0);
        }
    }

    public NewmanAgent() {
        this.config = new NewmanAgentConfig();
        this.workers = new ThreadPoolExecutor(config.getNumOfWorkers(), config.getNumOfWorkers(),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        this.active = true;
    }

    private void initialize(boolean withShutdownHook) {
        logger.info("Agent is initializing...");
        logger.info("Agent home dir is {}, please make sure no other agent is using this folder", config.getNewmanHome());
        while (true) {
            try {
                this.client = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                        config.getNewmanServerRestUser(), config.getNewmanServerRestPw());
                //try to connect to fail fast when server is down
                client.getJobs(1).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
        if (withShutdownHook) {
            attachShutDownHook();
        }
    }

    private void prepareJobSetupEnv(String suiteId, String buildId,String configId) throws ExecutionException, InterruptedException, IOException, TimeoutException {
        Suite suite = client.getSuite(suiteId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (suite == null) {
            throw new RuntimeException("no suite with id " + suiteId);
        }

        Build build = client.getBuild(buildId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (build == null) {
            throw new RuntimeException("no build with id " + buildId);
        }

        JobConfig jobConfig = client.getConfigById(configId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (jobConfig == null) {
            throw new RuntimeException("no jobConfig with id " + configId);
        }

        Job mockJob = new Job();
        mockJob.setBuild(build);
        mockJob.setSuite(suite);
        mockJob.setJobConfig(jobConfig);
        mockJob.setId("mock");

        JobExecutor jobExecutor = new JobExecutor(mockJob, config.getNewmanHome());
        boolean success = jobExecutor.setup();
        if (!success) {
            throw new RuntimeException("failed to execute job setup");
        }
        FileUtils.createFolder(append(jobExecutor.getJobFolder(), "test"));
        FileUtils.createFolder(append(append(jobExecutor.getJobFolder(), "test"), "output"));
        logger.info("##############################################################");
        logger.info("Created job setup environment in " + jobExecutor.getJobFolder());
        logger.info("Before running manager tests, edit ../run-sgtest.sh, add: 'export SUITE_SUB_TYPE=\"manager\"' at the top of the script, under 'export SGTEST_DIR'");
        logger.info("Example of running a test: cd " + jobExecutor.getJobFolder() + "/test; ../run-sgtest.sh <test-name>");
        logger.info("After test ended run: ../end-sgtest.sh");
        logger.info("##############################################################");
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
        this.name = config.getHostAddress() + "-" + UUID.randomUUID().toString();
    }

    private void start() {
        NewmanClient c = getClient();
        KeepAliveTask keepAliveTask = null;
        Job prevJob = null;
        JobExecutor jobExecutor = null;

/*        try {
            c.deleteSuite("5d6e945d968ec30a23c9b11e").toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            System.out.println("success");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }*/

        while (isActive()) {
            boolean toStopafterOneIterate = false;
            Agent agent;

            if(prevJob != null){
                toStopafterOneIterate = true;
            }

            final Job job = waitForJob(toStopafterOneIterate);

            if(job == null){
                keepAliveTask.cancel();
                jobExecutor.teardown();
                prevJob = null;
                continue;
            }

            if (prevJob != null && job.getId().equals(prevJob.getId())) {
                logger.info("Job {}: The same job was found, no need to setup job again", job.getId());
                prevJob = null;

            } else {
                if (prevJob != null) {
                    try {
                        prevJob = c.agentFinishJob(prevJob.getId()).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.warn("Failed to change the state of job {}: {} : ", prevJob, e);
                    }
                    keepAliveTask.cancel();
                    jobExecutor.teardown();
                }

                // ping server during job setup and execution
                keepAliveTask = startKeepAliveTask(job.getId(), keepAliveTask);
                jobExecutor = new JobExecutor(job, config.getNewmanHome());
                boolean setupFinished = jobExecutor.setup();
                reportJobSetup(job.getId(), name, jobExecutor.getJobFolder());

                if (!setupFinished) {
                    logger.error("Setup of job {} failed, will wait for a new job", job.getId());
                    jobExecutor.teardown();
                    //inform the server that agent is not working on this job
                    try {
                        agent = c.getAgent(name).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS , TimeUnit.SECONDS);
                        c.unsubscribe(agent).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        c.setSetupRetries(agent, agent.getSetupRetries() + 1).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (IllegalStateException e) {
                        c = getClient();
                    } catch (Exception e) {
                        logger.warn("Failed to unsubscribe agent {} due to failure in job setup of job {}: {}", name, job.getId(), e);
                    }
                    try {
                        Thread.sleep(config.getJobPollInterval());
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
            }
            try {
                agent = c.getAgent(name).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                c.setSetupRetries(agent, 0).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Failed to find agent: " + name + ", retrying in " + config.getJobPollInterval() + "milliseconds");
                try {
                    Thread.sleep(config.getJobPollInterval());
                } catch (InterruptedException ignored) {
                }
                continue;
            }

            lastPriorityCheckedTime = System.currentTimeMillis();
            workerShouldStop = false;
            // Submit workers:
            final Agent currentAgent = agent;

            final JobExecutor currJobExecutor = jobExecutor;
            List<Future<?>> workersTasks = new ArrayList<>();
            for (int i = 0; i < calculateNumberOfWorkers(job); i++) {
                final int id = i;

                Future<?> worker = workers.submit(()-> {
                        logger.info("Starting worker #{} for job {}", id, currJobExecutor.getJob().getId());
                        Test test;
                        while(!workerShouldStop){
                            long timeNow = System.currentTimeMillis();
                            if((timeNow - lastPriorityCheckedTime) > (MILLISECONDS_IN_SECOND * 60)){
                               workerShouldStop = hasPrioritizedJob(currentAgent.getId(), job.getId());
                                lastPriorityCheckedTime = System.currentTimeMillis();
                                if(workerShouldStop){
                                    break;
                                }
                            }

                            if((test = findTest(currJobExecutor.getJob())) != null){
                                Test testResult = currJobExecutor.run(test);
                                if (testResult.getStatus().equals(Test.Status.FAIL)) {
                                    resubmitFailed(job.getId(), testResult);
                                }
                                reportTest(testResult);
                            }
                            else{
                              break;
                            }
                        }
                        logger.info("Finished Worker #{} for job {}", id, currJobExecutor.getJob().getId());
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

            if (!workerShouldStop) {
                keepAliveTask.cancel();
                jobExecutor.teardown();
                prevJob = null;
            } else {
                prevJob = job;
                logger.info("Job {} changed before it's finished, because there is a job in higher priority", job.getId());
            }
        }
    }


    private boolean hasPrioritizedJob(final String agentId, final String jobId){
        try {
            if(client.hasHigherPriorityJob(agentId, jobId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
                return true;
            }
        } catch (Exception e) {
            logger.info("Failed to check if there is a job with higher priority: " + e);
        }

        return false;
    }

    private void resubmitFailed(String jobId, Test failedTest){
        Job job = null;
        try {
            job = client.getJob(jobId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        if(job == null){
            return;
        }
        if(failedTest.getRunNumber() > 2 || job.getFailedTests() > 100){
            return;
        }
        List<Test> tests = new ArrayList<>(1);
            Test newTest = new Test();
            newTest.setName(failedTest.getName());
            newTest.setArguments(failedTest.getArguments());
            newTest.setTestType(failedTest.getTestType());
            newTest.setTimeout(1500000L);
            newTest.setJobId(failedTest.getJobId());
            newTest.setRunNumber(failedTest.getRunNumber() + 1);
            tests.add(newTest);
        try {
            client.createTests(tests, "don't count").toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS * 5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
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

    private void close() {
        if (isActive()) {
            active = false;
            logger.info("Closing newman agent {}", name);
            shutDownWorkers();
            if (client != null) {
                try {
                    client.freeAgent(name).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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

    private Job waitForJob(boolean toStopAfterOneIterate) {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setHost(config.getHostName());
        agent.setHostAddress(config.getHostAddress());
        agent.setPid(ProcessUtils.getProcessId("unknownPID"));
        agent.setGroupName(config.getGroupName());
        String capabilities = config.getCapabilities();
        // TODO note - if set is empty, mongodb will NOT write that filed to DB
        agent.setCapabilities(CapabilitiesAndRequirements.parse(capabilities));
        int logRepeats = 3;
        NewmanClient c = getClient();
        while (true) {
            try {
                ServerStatus serverStatus = c.getServerStatus().toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (serverStatus.getStatus().equals(ServerStatus.Status.SUSPENDED)) {
                    logger.info("Server is suspended, will retry in " +config.getRetryIntervalOnSuspended()/1000+ " seconds");
                    Thread.sleep(config.getRetryIntervalOnSuspended());
                    continue;
                }


                Job job = c.subscribe(agent).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);
                if (job != null || toStopAfterOneIterate)
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
            catch (ExecutionException e) {
                if (e.getCause() instanceof WebApplicationException){
                    WebApplicationException ex = (WebApplicationException) e.getCause();
                    String responseText = ex.getResponse().readEntity(String.class);
                    logger.warn("Agent failed while polling newman-server. Got status: " + ex.getResponse().getStatus()+", message: " + responseText);
                    c = getClient();
                } else {
                    c = onClientFailure(c);
                }
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

    private synchronized NewmanClient onClientFailure(NewmanClient localClient) {
        long lastTimeFailedToConnectToServer = System.currentTimeMillis();
        long minutesToPassBeforeExit = 1;
        if (localClient != getClient()) { // in case other thread already restarted the client
            return getClient();
        }

        if (client != null) {
            client.close();
        }
        while (true) {
            long currentTime =  System.currentTimeMillis();
            long timePassedSinceStartToFail = currentTime - lastTimeFailedToConnectToServer;
            long timePassedSinceStartToFailMinutes = TimeUnit.MILLISECONDS.toMinutes(timePassedSinceStartToFail);
            if(timePassedSinceStartToFailMinutes >= minutesToPassBeforeExit){
                logger.error("agent {} can't connect to server host: {}, port: {}. Existing agent...", name, config.getNewmanServerHost(), config.getNewmanServerPort());
                throw new IllegalArgumentException("agent did not connect for the last " + timePassedSinceStartToFailMinutes + " minutes, restarting agent...");
            }
            try {
                logger.info("handling client failure, reconnecting to newman server ip: {}, port: {}", config.getNewmanServerHost(), config.getNewmanServerPort());
                client = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                        config.getNewmanServerRestUser(), config.getNewmanServerRestPw());
                //try to connect to fail fast when server is down
                client.ping(name).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                break;
            } catch (Exception e) {
                logger.warn("failure while recreation of newman client, will retry...", e);
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e1) {
                    logger.error("Thread got exception while trying to sleep. ", e1);
                }
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
                Test finishedTest = c.finishTest(testResult).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                logger.info("finished test [{}].", finishedTest);
                Path logs = append(config.getNewmanHome(), "logs");
                Path testLogsFile = append(logs, "output-" + testResult.getId() + ".zip");
                Test testLog = c.uploadTestLog(testResult.getJobId(), testResult.getId(), testLogsFile.toFile()).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS*4, TimeUnit.SECONDS);
                logger.info("update log testLog [{}].", testLog);
                break;
            }
            catch (IllegalStateException e){ // client was closed
                logger.warn("Got IllegalStateException while reporting test" , e);
                c = getClient();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof WebApplicationException){
                    WebApplicationException ex = (WebApplicationException) e.getCause();
                    String responseText = ex.getResponse().readEntity(String.class);
                    logger.warn("Agent failed while updating test result. Got status: " + ex.getResponse().getStatus()+", message: " + responseText);
                    c = getClient();
                    break;
                } else {
                    logger.warn("Got ExecutionException" , e);
                    c = onClientFailure(c);
                }
            } catch (Exception e) {
                logger.warn("Worker failed to update test result: {} caught: {}", testResult, e);
                c = onClientFailure(c);
            }
        }
    }

    private void reportJobSetup(String jobId, String agentName,  Path jobFolder) {
        NewmanClient c = getClient();
        logger.info("Reporting job setup log {}", jobId);
        Path logFile = append(jobFolder, "job-setup.log");
        try {
            c.uploadJobSetupLog(jobId,agentName, logFile.toFile()).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS*4, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to upload job setup log", e);
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


