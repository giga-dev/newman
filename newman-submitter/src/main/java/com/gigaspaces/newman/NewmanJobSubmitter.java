package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.criteria.CriteriaEvaluator;
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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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


    public NewmanJobSubmitter(String suiteId, String buildId, String host, String port, String username, String password){

        this.buildId = buildId;
        this.suiteId = suiteId;

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    public String submitJob() throws ExecutionException, InterruptedException, IOException, ParseException, TimeoutException {
        try {
            Suite suite = null;
            try {
                suite = newmanClient.getSuite(suiteId).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.error("can't get suite to submit. exception: {}" , e);
                throw  e;
            }
            if (suite == null) {
                throw new IllegalArgumentException("suite with id: " + suiteId + " does not exists");
            }
            Build build = null;
            try{
                build = newmanClient.getBuild(buildId).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            catch (TimeoutException e){
                logger.error("can't get build to submit. exception: {}" , e);
                throw  e;
            }
            if (build == null) {
                throw new IllegalArgumentException("build with id: " + buildId + " does not exists");
            }

            validateUris(build.getTestsMetadata()); // throws exception if URI not exists

            Job job = addJob(newmanClient, suiteId, buildId);
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


    private Job addJob(NewmanClient client, String suiteId, String buildId) throws ExecutionException, InterruptedException, TimeoutException {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setBuildId(buildId);
        jobRequest.setSuiteId(suiteId);
        try {
            return client.createJob(jobRequest).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("can't create job: suiteId: [{}], buildId:[{}]. exception: {}", suiteId, buildId, e);
            throw e;
        }
    }

    private void addTests(List<Test> tests, NewmanClient client) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            client.createTests(tests).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS * 5, TimeUnit.SECONDS);
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

    //0- suiteId, 1-buildId, 2-host, 3- port, 4- user, 5- password
    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException, ParseException, TimeoutException {

        if (args.length != 6){
            logger.error("Usage: java -cp newman-submitter-1.0.jar com.gigaspaces.newman.NewmanJobSubmitter <suiteid> <buildId>" +
                    " <newmanServerHost> <newmanServerPort> <newmanUser> <newmanPassword>");
            System.exit(1);
        }
        //TODO validate arguments
        String suiteId = args[0];
        String buildId = args[1];
        String host = args[2];
        String port = args[3];
        String username = args[4];
        String password = args[5];

        NewmanJobSubmitter submitter = new NewmanJobSubmitter(suiteId, buildId, host, port, username, password);

        final String jobId = submitter.submitJob();

        logger.info("Submitted a new job with id {}", jobId);
    }
}
