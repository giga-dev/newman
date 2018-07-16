package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.FutureJob;
import com.gigaspaces.newman.utils.EnvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by tamirs
 * on 10/21/15.
 */
public class NewmanFutureJobSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanFutureJobSubmitter.class);

    private static final String NEWMAN_BUILD_ID = "NEWMAN_BUILD_ID"; // for example: 56277de629f67f791db25554
    private static final String NEWMAN_SUITE_ID = "NEWMAN_SUITE_ID"; // for example: 55b0affe29f67f34809c6c7b
    private static final String NEWMAN_CONFIG_ID = "NEWMAN_CONFIG_ID"; // for example: 55b0affe29f67f34809c6c7b
    private static final String AUTHOR = "AUTHOR"; // for example: tamirs


    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException, TimeoutException {

        // NOTE - need to pass system argument!

        NewmanClient newmanClient;
        String build_id;
        String suite_id;
        String config_id;
        String author;

        newmanClient = getNewmanClient();
        build_id = EnvUtils.getEnvironment(NEWMAN_BUILD_ID, true, logger);
        suite_id = EnvUtils.getEnvironment(NEWMAN_SUITE_ID, true, logger);
        config_id = EnvUtils.getEnvironment(NEWMAN_CONFIG_ID, true, logger);
        author = EnvUtils.getEnvironment(AUTHOR, true, logger);

        ServerStatus serverStatus = newmanClient.getServerStatus().toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!serverStatus.getStatus().equals(ServerStatus.Status.RUNNING)) {
            logger.error("Server is "+serverStatus.getStatus()+". Please try again later");
            System.exit(1);
        }

        validBuildAndSuite(newmanClient, build_id, suite_id);
        validateJobConfig(newmanClient,config_id);
        FutureJob futureJob;
        try {
            futureJob = newmanClient.createFutureJob(build_id, suite_id, config_id, author).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("can't create future job. execption: {}", e);
            throw e;
        }
        logger.info("{} creates futureJob with id: {}, build id: {}, and suite id: {}.",futureJob.getAuthor(), futureJob.getId(), futureJob.getBuildID(), futureJob.getSuiteID());
    }


    private static void validBuildAndSuite(NewmanClient newmanClient, String build_id, String suite_id){
        try{
            newmanClient.getBuild(build_id).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }catch (Exception e){
            throw new RuntimeException("build id : "+ build_id +" does not exist");
        }
        try{
            newmanClient.getSuite(suite_id).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }catch (Exception e){
            throw new RuntimeException("suite id: "+ suite_id +" does not exist");
        }
    }

    private static void validateJobConfig(NewmanClient newmanClient,String configId){
        try{
            newmanClient.getConfigById(configId).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }catch (Exception e){
            throw new RuntimeException("Config id : "+ configId +" does not exist");
        }
    }

    private static NewmanClient getNewmanClient() {
        // connection arguments
        NewmanClient nc;
        String host = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_HOST, true /*required*/, logger);
        String port = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_PORT, true /*required*/, logger);
        String username = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_USER_NAME, true /*required*/, logger);
        String password = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_PASSWORD, true /*required*/, logger);

        try {
            nc = NewmanClient.create(host, port, username, password);
            //try to connect to fail fast when server is down
            nc.getJobs().toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("newmanClient did not connect, check if server up and arguments");
        }
        return nc;
    }



}
