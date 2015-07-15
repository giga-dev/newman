package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.State;
import com.gigaspaces.newman.beans.Suite;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

/**
 @author Boris
 @since 1.0
 */

public class NewmanSubmitter {

    private static final String NEWMAN_HOST = "NEWMAN_HOST";
    private static final String NEWMAN_PORT = "NEWMAN_PORT";
    private static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    private static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";
    private static final String NEWMAN_SUITE = "NEWMAN_SUITE";
    private static final Logger logger = LoggerFactory.getLogger(NewmanSubmitter.class);

    private NewmanClient newmanClient;
    private String suiteId;
    private NewmanBuildMetadata buildMetadata;
    private String host;
    private String port;
    private String username;
    private String password;

    public NewmanSubmitter(String suiteId, NewmanBuildMetadata buildMetadata, String host, String port, String username, String password){

        this.suiteId = suiteId;
        this.buildMetadata = buildMetadata;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    public void work() throws ExecutionException, InterruptedException, IOException, ParseException {
        try {
            Suite suite = newmanClient.getSuite(suiteId).toCompletableFuture().get();
            if (suite == null) {
                throw new IllegalArgumentException("suite with id: " + suiteId + " does not exists");
            }

            NewmanBuildSubmitter buildSubmitter = new NewmanBuildSubmitter(buildMetadata, host, port, username, password);

            String buildId = buildSubmitter.submitBuild();

            final NewmanJobSubmitter jobSubmitter = new NewmanJobSubmitter(suiteId, buildId, host, port, username, password);

            String jobId = jobSubmitter.submitJob();

            while (!isJobFinished(jobId)) {
                logger.info("waiting for job {} to end", jobId);
                Thread.sleep(10 * 1000);
            }
        }
        finally {
            if (newmanClient != null)
                newmanClient.close();
        }
    }

    private boolean isJobFinished(String jobId) {
        try {
            Job job = null;
            final Batch<Job> jobBatch = newmanClient.getJobs().toCompletableFuture().get();
            for (Job j : jobBatch.getValues()) {
                if (j.getId().equals(jobId)){
                    job = j;
                    break;
                }
            }
            if (job == null){
                throw new IllegalArgumentException("No such job with id: " + jobId);
            }
            return job.getState() == State.DONE;

        } catch (Exception e) {
            logger.error("failed to check if job is finished", e);
            return false;
        }
    }

    public static String getEnvironment(String var) {
        String v = System.getenv(var);
        if (v == null){
            logger.error("Please set the environment variable {} and try again.", var);
            throw new IllegalArgumentException("the environment variable " + var + " must be set");
        }
        return v;
    }

    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException, ParseException {
        String publishFolder = getEnvironment(NewmanBuildMetadata.BUILD_S3_PUBLISH_FOLDER);
        String newmanBuildMilestone = getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_MILESTONE);
        String newmanBuildVersion = getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_VERSION);
        String buildBranch = getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_BRANCH);
        String buildNumber = getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_NUMBER);
        // connection arguments
        String host = getEnvironment(NEWMAN_HOST);
        String port = getEnvironment(NEWMAN_PORT);
        String username = getEnvironment(NEWMAN_USER_NAME);
        String password = getEnvironment(NEWMAN_PASSWORD);
        // suite to run
        String suiteId = getEnvironment(NEWMAN_SUITE);

        NewmanBuildMetadata buildMetadata = new NewmanBuildMetadata(publishFolder, newmanBuildMilestone, newmanBuildVersion, buildBranch, buildNumber);

        NewmanSubmitter newmanSubmitter = new NewmanSubmitter(suiteId, buildMetadata, host, port, username, password);
        newmanSubmitter.work();
    }
}
