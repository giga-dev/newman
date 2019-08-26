package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.FutureJob;
import com.gigaspaces.newman.beans.FutureJobsRequest;
import com.gigaspaces.newman.utils.EnvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private static final String NEWMAN_AGENT_GROUPS = "NEWMAN_AGENT_GROUPS"; // for example: "devGroup,imc-srv01"
    private static final String NEWMAN_PRIORITY = "NEWMAN_PRIORITY";
    private static final String AUTHOR = "AUTHOR"; // for example: tamirs

    private static NewmanClient newmanClient = getNewmanClient();

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {

        // NOTE - need to pass system argument!

        String build_id;
        String suite_id;
        String config_id;
        String author;
        String priorityNum;

        build_id = EnvUtils.getEnvironment(NEWMAN_BUILD_ID, true, logger);
        suite_id = EnvUtils.getEnvironment(NEWMAN_SUITE_ID, true, logger);
        config_id = EnvUtils.getEnvironment(NEWMAN_CONFIG_ID, true, logger);
        author = EnvUtils.getEnvironment(AUTHOR,true, logger);
        priorityNum = EnvUtils.getEnvironment(NEWMAN_PRIORITY, true, logger);
        int priority = Integer.parseInt(priorityNum);
        List<String> suites = parse(suite_id);
        String requiredAgentGroups = EnvUtils.getEnvironment(NEWMAN_AGENT_GROUPS, true, logger);
        Set<String> agentGroups = new TreeSet<>(parse(requiredAgentGroups));

        ServerStatus serverStatus = newmanClient.getServerStatus().toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!serverStatus.getStatus().equals(ServerStatus.Status.RUNNING)) {
            logger.error("Server is "+serverStatus.getStatus()+". Please try again later");
            System.exit(1);
        }

        validBuildAndSuite(build_id, suites);
        validateJobConfig(config_id);

        NewmanFutureJobSubmitter futureJobSubmitter = new NewmanFutureJobSubmitter();

        FutureJobsRequest futureJobRequest = new FutureJobsRequest();
        futureJobRequest.setBuildId(build_id);
        futureJobRequest.setSuites(suites);
        futureJobRequest.setConfigId(config_id);
        futureJobRequest.setAuthor(author);
        futureJobRequest.setAgentGroups(agentGroups);
        futureJobRequest.setPriority(priority);

        List<FutureJob> futureJobs = futureJobSubmitter.submitFutureJobs(futureJobRequest);

        for(FutureJob futureJob: futureJobs){
            logger.info("new futureJob was created: " + futureJob);
        }
    }

    private List<FutureJob> submitFutureJobs(FutureJobsRequest futureJobRequest) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            return newmanClient.createFutureJob(futureJobRequest).toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("can't create future job. execption: {}", e);
            throw e;
        }
    }


    private static void validBuildAndSuite(String build_id, List<String> suites_id){
        try{
            newmanClient.getBuild(build_id).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }catch (Exception e){
            throw new RuntimeException("build id : "+ build_id +" does not exist");
        }

        for(String suite : suites_id){
            try{
                newmanClient.getSuite(suite).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }catch (Exception e){
                throw new RuntimeException("suite id: "+ suite +" does not exist");
            }
        }
    }

    private static void validateJobConfig(String configId){
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
           // nc.getJobs(1).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("newmanClient did not connect, check if server up and arguments", e);
        }
        return nc;
    }

    private static List<String> parse(String input){
        List<String> output =  new ArrayList<>();
        if(input  != null) {
            StringTokenizer st = new StringTokenizer(input, ",");
            while (st.hasMoreTokens())
                output.add(st.nextToken());
        }
        return output;
    }
}
