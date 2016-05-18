package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.utils.EnvUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;
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

    public boolean submitFutureJobsIfAny() throws InterruptedException, ExecutionException, TimeoutException {
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

    public void submitAndWait(String buildId, String suitesIDStr) throws InterruptedException, ExecutionException, IOException, TimeoutException {
        List<Future<String>> jobs = new ArrayList<>();
        // Submit jobs for suites and wait for them
        logger.info("Using build with id: {}", buildId);
        List<String> suitesId = Arrays.asList(suitesIDStr.split(","));
        try {
            for (String suiteId : filterSuites(suitesId, buildId)) {
                Future<String> worker = submitJobsByThreads(suiteId, buildId, username);
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

    private List<String> filterSuites(List<String> suitesId, String buildId) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            List<Suite> staticMiniSuites = newmanClient.getAllSuites().toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS).getValues();
            Build build = newmanClient.getBuild(buildId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<String> filteredSuites = new ArrayList<>();
            for (Suite staticSuite : staticMiniSuites) {
                if (suitesId.contains(staticSuite.getId())){
                    Map<String, String> CustomVariablesMap = Suite.parseCustomVariables(staticSuite.getCustomVariables());
                    String requireBuildStr = CustomVariablesMap.get("REQUIRE_BUILD_TAG");
                    Set<String> requireTags = new HashSet<>();
                    if(requireBuildStr != null){
                        requireTags = new HashSet<>(Arrays.asList(requireBuildStr.split(",")));
                    }
                    if(build.getTags().containsAll(requireTags)){
                        filteredSuites.add(staticSuite.getId());
                    }
                }
            }
            return filteredSuites;
        } catch (Exception e) {
            logger.error("can't filter suites. exception: {}", e);
            throw  e;
        }
    }

    /**
     * @return List of Future jobs ids.
     */
    private List<Future<String>> submitFutureJobs() throws InterruptedException, ExecutionException, TimeoutException {
        List<Future<String>> futureJobIds = new ArrayList<>();
        FutureJob futureJob = getAndDeleteFutureJob();
        while(futureJob != null){
            Future<String> futureJobWorker = submitJobsByThreads(futureJob.getSuiteID(), futureJob.getBuildID(), futureJob.getAuthor());
            futureJobIds.add(futureJobWorker);
            futureJob = getAndDeleteFutureJob();
        }
        return futureJobIds;
    }

    private Future<String> submitJobsByThreads(String suiteId, String buildId, String author){
        return workers.submit(() -> {
            Suite suite = null;
            try {
                suite = newmanClient.getSuite(suiteId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (suite == null) {
                    throw new IllegalArgumentException("job suite with id: " + suiteId + " does not exists");
                }
                final NewmanJobSubmitter jobSubmitter = new NewmanJobSubmitter(suiteId, buildId, host, port, username, password);

                String jobId = jobSubmitter.submitJob(author);

                while (!isJobFinished(jobId)) {
                    logger.info("waiting for job {} to end", jobId);
                    Thread.sleep(60 * 1000);
                }
                return jobId;
            } catch (InterruptedException | ExecutionException | ParseException | IOException e) {
                throw new RuntimeException("job terminating submission due to exception", e);
            }
        });
    }


    private FutureJob getAndDeleteFutureJob() throws InterruptedException, ExecutionException, TimeoutException {
        FutureJob futureJob = null;
        try {
            futureJob = newmanClient.getAndDeleteOldestFutureJob().toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("failed to submit future job and delete it", e);
            throw e;
        }
        return futureJob;
    }

    private boolean isJobFinished(String jobId) {
        try {
            final Job job = newmanClient.getJob(jobId).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (job == null){
                throw new IllegalArgumentException("No such job with id: " + jobId);
            }
            return job.getState() == State.DONE || job.getState() == State.BROKEN;

        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("failed to check if job is finished. exception: {}", e);
            return false;
        }
    }

    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException, ParseException, TimeoutException {
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
        // NOTE exit code = -1 if there are future jobs
        if (hasFutureJobs) {
            newmanSubmitter.tearDown();
            System.exit(-1);
        }

        String buildIdToRun = newmanSubmitter.getBuildToRun(branch, tags, mode);
        logger.info("build to run - id:[{}], branch:[{}], tags:[{}], mode:[{}].",buildIdToRun, branch, tags, mode);
        if(buildIdToRun != null){
            try{
                Build cachedBuild = newmanSubmitter.newmanClient.cacheBuildInServer(buildIdToRun).toCompletableFuture().get();
                newmanSubmitter.submitAndWait(cachedBuild.getId(), suitesId);
            }
            catch (Exception ignored){
                // TODO if build is not valid tag it as BROKEN
                logger.error("Not succeeding cache or submit job. build id- ["+ buildIdToRun +"] on branch :[" + branch +"]");
            }
        }
        System.exit(0);
    }

    public String getBuildToRun(String branch, String tags, String mode){
        try {
            if(mode == null || mode.isEmpty()){
                mode = "DAILY";
            }
            // tags should be separated by comma (,)
            Build build = newmanClient.getBuildToSubmit(branch, tags, mode).toCompletableFuture().get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if(build != null) {
                return build.getId();
            }
            logger.warn("failed to find build on branch: {}, tags: {}, mode: {}", branch, tags, mode);
            return null;

        } catch (Exception e) {
            logger.error("failed to get build to run: {}, tags: {}, mode: {}, exception: {}", branch, tags, mode, e);
            return null;
        }
    }

}
