package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.utils.EnvUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 @author Boris
 @since 1.0
 */

public class NewmanSubmitter {

    private static final String NEWMAN_HOST = "NEWMAN_HOST";
    private static final String NEWMAN_PORT = "NEWMAN_PORT";
    private static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    private static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";
    private static final String NEWMAN_SUITES = "NEWMAN_SUITES";
    private static final int MAX_THREADS = 20;
    private static final String NEWMAN_BUILD_BRANCH = "NEWMAN_BUILD_BRANCH";
    private static final String NEWMAN_BUILD_TAGS = "NEWMAN_BUILD_TAGS";
    // modes = FORCE, REGULAR
    private static final String NEWMAN_MODE = "NEWMAN_MODE";

    private static final Logger logger = LoggerFactory.getLogger(NewmanSubmitter.class);

    private NewmanClient newmanClient;
    private String host;
    private String port;
    private String username;
    private String password;
    private ThreadPoolExecutor workers;

    public NewmanSubmitter(String host, String port, String username, String password){

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.workers = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    public boolean submitFutureJobsIfAny(){
        // Submit future jobs if exists
        List<Future<String>> jobs = submitFutureJobs();
        if(jobs.isEmpty()){ // if there are no future jobs
            return false;
        }else{
            // wait for every running future job to finish.
            for (Future<String> job : jobs) {
                try {
                    job.get();
                }catch (Exception e){
                    logger.warn("main thread catch exception of worker: ", e);
                }
            }
            return true;
        }
    }

    private void tearDown() {
        if (newmanClient != null)
            newmanClient.close();
        // shut down job submitters workers
        workers.shutdown();
        try {
            if (!workers.awaitTermination(30, TimeUnit.SECONDS)) {
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

    public void submitAndWait(String buildId, String suitesIDStr) throws InterruptedException, ExecutionException, IOException {
        List<Future<String>> jobs = new ArrayList<>();
        // Submit jobs for suites and wait for them
        logger.info("Using build with id: {}", buildId);
        List<String> suites = Arrays.asList(suitesIDStr.split(","));
        try {
            for (String suiteId : suites) {
                Future<String> worker = submitJobsByThreads(suiteId, buildId);
                jobs.add(worker);
            }
            // wait for every running job to finish.
            for (Future<String> job : jobs) {
                try {
                    job.get();
                } catch (Exception e) {
                    logger.warn("main thread catch exception of worker: ", e);
                }
            }
        }
        finally {
            tearDown();
        }
    }

    /**
     * @return List of Future jobs ids.
     */
    private List<Future<String>> submitFutureJobs(){
        List<Future<String>> futureJobIds = new ArrayList<>();
        FutureJob futureJob = getAndDeleteFutureJob();
        while(futureJob != null){
            Future<String> futureJobWorker = submitJobsByThreads(futureJob.getSuiteID(), futureJob.getBuildID());
            futureJobIds.add(futureJobWorker);
            futureJob = getAndDeleteFutureJob();
        }
        return futureJobIds;
    }

    private Future<String> submitJobsByThreads(String suiteId, String buildId){
        return workers.submit(() -> {
            Suite suite = null;
            try {
                suite = newmanClient.getSuite(suiteId).toCompletableFuture().get();
                if (suite == null) {
                    throw new IllegalArgumentException("future job suite with id: " + suiteId + " does not exists");
                }
                final NewmanJobSubmitter jobSubmitter = new NewmanJobSubmitter(suiteId, buildId, host, port, username, password);

                String jobId = jobSubmitter.submitJob();

                while (!isJobFinished(jobId)) {
                    logger.info("waiting for job {} to end", jobId);
                    Thread.sleep(60 * 1000);
                }
                return jobId;
            } catch (InterruptedException | ExecutionException | ParseException | IOException e) {
                throw new RuntimeException("future job terminating submission due to exception", e);
            }
        });
    }


    private FutureJob getAndDeleteFutureJob(){
        FutureJob futureJob = null;
        try {
            futureJob = newmanClient.getAndDeleteOldestFutureJob().toCompletableFuture().get();
        } catch (Exception e) {
            logger.error("failed to submit future job and delete it");
        }
        return futureJob;
    }

    private boolean isJobFinished(String jobId) {
        try {
            final Job job = newmanClient.getJob(jobId).toCompletableFuture().get();
            if (job == null){
                throw new IllegalArgumentException("No such job with id: " + jobId);
            }
            return job.getState() == State.DONE;

        }
        catch (InterruptedException | ExecutionException e) {
            logger.error("failed to check if job is finished", e);
            return false;
        }
    }

    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException, ParseException {
        // connection arguments
        String host = EnvUtils.getEnvironment(NEWMAN_HOST, logger);
        String port = EnvUtils.getEnvironment(NEWMAN_PORT, logger);
        String username = EnvUtils.getEnvironment(NEWMAN_USER_NAME, logger);
        String password = EnvUtils.getEnvironment(NEWMAN_PASSWORD, logger);
        // suites to run separated by comma
        String suitesId = EnvUtils.getEnvironment(NEWMAN_SUITES, logger);

        NewmanSubmitter newmanSubmitter = new NewmanSubmitter(host, port, username, password);
        String branch = EnvUtils.getEnvironment(NEWMAN_BUILD_BRANCH, false, logger);
        String tags = EnvUtils.getEnvironment(NEWMAN_BUILD_TAGS, false, logger);
        String mode = EnvUtils.getEnvironment(NEWMAN_MODE, false, logger);

        logger.info("submitting future jobs if there are any");
        boolean hasFutureJobs = newmanSubmitter.submitFutureJobsIfAny();
        logger.info("hasFutureJobs: {}", hasFutureJobs);
        if (hasFutureJobs) {
            newmanSubmitter.tearDown();
            System.exit(1);
        }

        String buildToRun = newmanSubmitter.getBuildToRun(branch, tags, mode);
        logger.info("submitter branch: {}, found buildToRun: {}", branch, buildToRun);
        if(buildToRun != null){
            newmanSubmitter.submitAndWait(buildToRun, suitesId);
        }
        System.exit(0);
    }

    public String getBuildToRun(String branches, String tags, String mode){
        if(mode.equals("DAILY")){
            try {
                // branches and tags should be separated by comma (,)
                List<Build> buildsNotRunYet = newmanClient.getPendingBuildsToSubmit(branches, tags).toCompletableFuture().get().getValues();
                if(buildsNotRunYet != null && !buildsNotRunYet.isEmpty()) { //found build to run
                    Build build =  buildsNotRunYet.get(0);
                    return build.getId();
                }
                logger.warn("failed to find build on branch: {}, tags: {}, mode: {}", branches, tags, mode);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        else if(mode.equals("NIGHTLY")){// mode = NIGHTLY
            try {
                Build build = newmanClient.getLatestBuild(tags).toCompletableFuture().get();
                if(build != null){
                    return build.getId();
                }
                logger.warn("failed to find build on branch: {}, tags: {}, mode: {}", branches, tags, mode);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        throw new IllegalArgumentException("illegal mode argument: " + mode);
    }

}
