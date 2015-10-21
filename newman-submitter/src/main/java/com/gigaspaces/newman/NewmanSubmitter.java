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
    private static final String NEWMAN_BUILD_ID = "NEWMAN_BUILD_ID";

    private static final Logger logger = LoggerFactory.getLogger(NewmanSubmitter.class);

    private NewmanClient newmanClient;
    private List<String> suites;
    private String host;
    private String port;
    private String username;
    private String password;
    private ThreadPoolExecutor workers;

    public NewmanSubmitter(String suitesId, String host, String port, String username, String password){

        this.suites = Arrays.asList(suitesId.split(","));
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.workers = new ThreadPoolExecutor(suites.size(), suites.size(),
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

    public void submitAndWait(String buildId) throws InterruptedException, ExecutionException, IOException {
        // Submit a new build if no buildId was provided, pay attention to provide all the environment variables for the build in that case
        // (see NewmanBuildSubmitter.getBuildMetadata method).
        final String bId = buildIfNeeded(buildId);
        // Use the below line with a specific tag to get the latest build which contains a tag
        //final String bId = newmanClient.getLatestBuild("tag").toCompletableFuture().get();
        logger.info("Using build with id: {}", buildId);
        // Submit jobs for suites and wait for them
        try {
            List<Future<?>> jobs = new ArrayList<>();
            for (String suiteId : suites) {
                Future<?> worker = workers.submit(() -> {
                    Suite suite = null;
                    try {
                        suite = newmanClient.getSuite(suiteId).toCompletableFuture().get();
                        if (suite == null) {
                            throw new IllegalArgumentException("suite with id: " + suites + " does not exists");
                        }
                        final NewmanJobSubmitter jobSubmitter = new NewmanJobSubmitter(suiteId, bId, host, port, username, password);

                        String jobId = jobSubmitter.submitJob();

                        while (!isJobFinished(jobId)) {
                            logger.info("waiting for job {} to end", jobId);
                            Thread.sleep(60 * 1000);
                        }
                    } catch (InterruptedException | ExecutionException | ParseException | IOException e) {
                        throw new RuntimeException("terminating submission due to exception", e);
                    }
                });
                jobs.add(worker);
            }
            for (Future<?> job : jobs) {
                job.get();
            }

        }
        finally {
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
    }

    private String buildIfNeeded(String buildId) throws IOException, ExecutionException, InterruptedException {
        if (buildId == null) {
            NewmanBuildSubmitter buildSubmitter = new NewmanBuildSubmitter(host, port, username, password);
            buildId = buildSubmitter.submitBuild();
        }
        return buildId;
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
        String buildId = EnvUtils.getEnvironment(NEWMAN_BUILD_ID, false, logger);

        NewmanSubmitter newmanSubmitter = new NewmanSubmitter(suitesId, host, port, username, password);
        newmanSubmitter.submitAndWait(buildId);
    }
}
