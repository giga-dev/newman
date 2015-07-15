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
import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    public String submitJob() throws ExecutionException, InterruptedException, IOException, ParseException {
        try {
            Suite suite = newmanClient.getSuite(suiteId).toCompletableFuture().get();
            if (suite == null) {
                throw new IllegalArgumentException("suite with id: " + suiteId + " does not exists");
            }

            Build build = newmanClient.getBuild(buildId).toCompletableFuture().get();
            if (build == null) {
                throw new IllegalArgumentException("build with id: " + buildId + " does not exists");
            }

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
                for (Test test : listOfTests) {
                    if (criteriaEvaluator.evaluate(test)) {
                        test.setJobId(job.getId());
                        addTest(test, newmanClient);
                    }
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

    private Job addJob(NewmanClient client, String suiteId, String buildId) throws ExecutionException, InterruptedException {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setBuildId(buildId);
        jobRequest.setSuiteId(suiteId);
        return client.createJob(jobRequest).toCompletableFuture().get();
    }

    private void addTest(Test test, NewmanClient client) throws ExecutionException, InterruptedException {
        client.createTest(test).toCompletableFuture().get();
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
    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException, ParseException {

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