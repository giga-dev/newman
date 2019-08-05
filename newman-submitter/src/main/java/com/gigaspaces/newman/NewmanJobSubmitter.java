package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.criteria.CriteriaEvaluator;
import com.gigaspaces.newman.utils.EnvUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.gigaspaces.newman.utils.FileUtils.validateUris;

/**
 @author Boris
 @since 1.0
 */

public class NewmanJobSubmitter {

    private static final Logger logger = LoggerFactory.getLogger(NewmanJobSubmitter.class);
    private NewmanClient newmanClient;
    private String buildId;
    private String suiteId;
    private String configId;
    private Set<String> requiredAgentGroups;

    public NewmanJobSubmitter(String suiteId, String buildId, String configId, String host, String port, String username, String password, Set<String> requiredAgentGroups){

        this.buildId = buildId;
        this.suiteId = suiteId;
        this.configId = configId;
        this.requiredAgentGroups =requiredAgentGroups;

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    public String submitJob(String author) throws ExecutionException, InterruptedException, IOException, ParseException, TimeoutException {
        try {

            ServerStatus serverStatus;
            try {
                serverStatus = newmanClient.getServerStatus().toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.error("can't get server status. exception: {}" , e);
                throw  e;
            }
            if (!serverStatus.getStatus().equals(ServerStatus.Status.RUNNING)) {
                logger.error("Server is "+serverStatus.getStatus()+". Please try again later");
                throw new IllegalStateException("Server is "+serverStatus.getStatus()+". Please try again later");
            }

            Suite suite = null;
            try {
                suite = newmanClient.getSuite(suiteId).toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.error("can't get suite to submit. exception: {}" , e);
                throw  e;
            }
            if (suite == null) {
                throw new IllegalArgumentException("suite with id: " + suiteId + " does not exists");
            }
            Build build = null;
            try{
                build = newmanClient.getBuild(buildId).toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e){
                logger.error("can't get build to submit. exception: {}" , e);
                throw  e;
            }
            if (build == null) {
                throw new IllegalArgumentException("build with id: " + buildId + " does not exists");
            }

            JobConfig jobConfig;
            try{
                jobConfig = newmanClient.getConfigById(configId).toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e){
                logger.error("can't get config to submit. exception: {}" , e);
                throw  e;
            }
            if (jobConfig == null) {
                throw new IllegalArgumentException("jobConfig with id: " + configId + " does not exists");
            }

            Set<String> availableAgentGroups;
            try{
                availableAgentGroups = newmanClient.getAvailableAgentGroups().toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e){
                logger.error("can't get agent group to submit. exception: {}" , e);
                throw  e;
            }

            Set<String> requiredNotAvailableAgentGroups = requiredAgentGroups.stream().filter(agentGroup -> !availableAgentGroups.contains(agentGroup)).collect(Collectors.toSet());
            if (!requiredNotAvailableAgentGroups.isEmpty()) {
                logger.error("The agent groups: " + requiredNotAvailableAgentGroups + " aren't available, continue to submit the job with all the required agent groups");
            }

            validateUris(build.getTestsMetadata()); // throws exception if URI not exists

            Job job = addJob(newmanClient, suiteId, buildId, configId, author, requiredAgentGroups);
            logger.info("added a new job {}", job);
            Collection<URI> testsMetadata = build.getTestsMetadata();

            if (testsMetadata == null) {
                logger.error("can't submit job when there is no tests metadata in the build [{}]", buildId);
                System.exit(1);
            }

            for (URI testMetadata : testsMetadata) {
                logger.info("parsing metadata file {}", testMetadata);
                List<Test> listOfTests = parseMetadata(testMetadata.toURL().openStream());
                Criteria criteria = suite.getCriteria();
                CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(criteria);
                List<Test> tests = new ArrayList<>();
                for (Test test : listOfTests) {
                    if (criteriaEvaluator.evaluate(test)) {
                        test.setJobId(job.getId());
                        tests.add(test);
                        if (tests.size() == 500) {
                            addTests(tests, newmanClient);
                            tests = new ArrayList<>();
                        }
                    }
                }
                if (!tests.isEmpty()){
                    addTests(tests, newmanClient);
                }
            }
            return job.getId();
        }
        finally {
            if (newmanClient != null){
                newmanClient.close();
            }
        }
    }

    private Job addJob(NewmanClient client, String suiteId, String buildId, String configId, String author, Set<String> agentGroups) throws ExecutionException, InterruptedException, TimeoutException {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setBuildId(buildId);
        jobRequest.setSuiteId(suiteId);
        jobRequest.setConfigId(configId);
        jobRequest.setAuthor(author);
        jobRequest.setAgentGroups(agentGroups);

        try {
            return client.createJob(jobRequest).toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("can't create job: suiteId: [{}], buildId:[{}], configId: [{}], agentGroups: [{}]. exception: {}", suiteId, buildId, configId, agentGroups, e);
            throw e;
        }
    }

    private void addTests(List<Test> tests, NewmanClient client) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            client.createTests(tests, "count").toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS * 5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("can't create tests. exception: {}", e);
            throw e;
        }
    }

    private List<Test> parseMetadata(InputStream is) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Reader in = null;
        try {
            in = new InputStreamReader(is);
            JSONObject metadataJson = (JSONObject) parser.parse(in);
            String type = (String) metadataJson.get("type");
            if (type == null)
                throw new IllegalArgumentException("metadata must have 'type' field");

            NewmanTestsMetadataParser newmanTestsMetadataParser = NewmanTestsMetadataParserFactory.create(type);
            return newmanTestsMetadataParser.parse(metadataJson);
        }
        finally {
            if (in != null)
                in.close();
        }
    }

    //0- suiteId, 1-buildId, 2-configId, 3-host, 4- port, 5- user, 6- password
    public static void main(String[] args) throws Exception {
        testSubmitterFromIntelliJ();

       //testSubmitterUsingArgs(args);
    }

    private static void testSubmitterFromIntelliJ() throws Exception {
        String suiteId = "59f25af7b3859424cac590b6";
        String buildId = "5d19f01a4cedfd000cd81982";
        String configId = "5b4c9342b3859411ee82c265";
        String requiredAgentGroups = "group1";

       if(requiredAgentGroups == null && requiredAgentGroups.isEmpty()){
           logger.error("missing input of required agent groups");
           System.exit(1);
       }

        String host = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_HOST, true /*required*/, logger);
        String port = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_PORT, true /*required*/, logger);
        String username = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_USER_NAME, true /*required*/, logger);
        String password = EnvUtils.getEnvironment(NewmanClientUtil.NEWMAN_PASSWORD, true /*required*/, logger);
        Set<String> agentGroups = NewmanJobSubmitter.parse(requiredAgentGroups);


        NewmanJobSubmitter submitter = new NewmanJobSubmitter(suiteId, buildId, configId, host, port, username, password, agentGroups);

        final String jobId = submitter.submitJob(username);

        logger.info("Submitted a new job with id {}", jobId);
    }

    //0- suiteId, 1-buildId, 2-configId, 3-host, 4- port, 5- user, 6- password, 7 - agentGroups
    private static void testSubmitterUsingArgs(String[] args) throws Exception{
        if (args.length != 16){
            logger.error("Usage: java -cp newman-submitter-1.0.jar com.gigaspaces.newman.NewmanJobSubmitter <suiteid> <buildId> <configId> <requiredAgentGroups>" +
                    " <newmanServerHost> <newmanServerPort> <newmanUser> <newmanPassword>");
            System.exit(1);
        }

        //TODO validate arguments
        String suiteId = args[1];
        String buildId = args[3];
        String configId = args[5];
        String host = args[7];
        String port = args[9];
        String username = args[11];
        String password = args[13];
        String requiredAgentGroups = args[15];

        if(requiredAgentGroups == null && requiredAgentGroups.isEmpty()){
            logger.error("missing input of required agent groups");
            System.exit(1);
        }

        Set<String> agentGroups = NewmanJobSubmitter.parse(requiredAgentGroups);
        NewmanJobSubmitter submitter = new NewmanJobSubmitter(suiteId, buildId, configId, host, port, username, password, agentGroups);

        final String jobId = submitter.submitJob(username);

        logger.info("Submitted a new job with id {}", jobId);
    }

    private static Set<String> parse(String input){
        Set<String> output =  new TreeSet<>();
        if(input  != null) {
            StringTokenizer st = new StringTokenizer(input, ",");
            while (st.hasMoreTokens())
                output.add(st.nextToken());
        }
        return output;
    }
}
