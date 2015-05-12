package com.gigaspaces.newman;


import com.gigaspaces.newman.beans.*;
import com.gigaspaces.newman.beans.criteria.Criteria;

import com.gigaspaces.newman.beans.criteria.CriteriaEvaluator;
import com.gigaspaces.newman.utils.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 @author Boris
 @since 1.0
 */

public class NewmanSubmitter {

    private static final Logger logger = LoggerFactory.getLogger(NewmanSubmitter.class);
    //0- suiteId, 1-buildId, 2-host, 3- port, 4- user, 5- password
    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException, ExecutionException, InterruptedException, ParseException {

        if (args.length != 6){
            logger.error("Usage: java -jar submitter.jar <suiteid> <buildId>" +
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

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        NewmanClient newmanClient = NewmanClient.create(host, port, username, password);

        Path home = FileUtils.append(System.getProperty("user.home"), "newman-submitter");
        FileUtils.createFolder(home);

        Suite suite = newmanClient.getSuite(suiteId).toCompletableFuture().get();
        if (suite == null){
            throw new IllegalArgumentException("suite with id: " + suiteId + " does not exists");
        }

        Build build = newmanClient.getBuild(buildId).toCompletableFuture().get();
        if (build == null){
            throw new IllegalArgumentException("build with id: " + buildId + " does not exists");
        }

        Job job = addJob(newmanClient, suiteId, buildId);
        logger.info("added a new job {}", job);
        Collection<URI> testsMetadata = build.getTestsMetadata();

        if (testsMetadata == null){
            logger.error("can't submit job when there is no tests metadata in the build [{}]", buildId);
            System.exit(1);
        }

        for(URI testMetadata : testsMetadata) {
            Path metadataFile = FileUtils.download(testMetadata.toURL(), home);
            logger.info("parsing metadata file {}", metadataFile);
            List<Test> listOfTests = parseMetadata(metadataFile.toFile());
            Criteria criteria = suite.getCriteria();
            CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(criteria);
            for (Test test : listOfTests) {
                if (criteriaEvaluator.evaluate(test)) {
                    test.setJobId(job.getId());
                    addTest(test, newmanClient);
                }
            }
        }
    }

    private static Job addJob(NewmanClient client, String suiteId, String buildId) throws ExecutionException, InterruptedException {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setBuildId(buildId);
        jobRequest.setSuiteId(suiteId);
        return client.createJob(jobRequest).toCompletableFuture().get();
    }

    private static void addTest(Test test, NewmanClient client) throws ExecutionException, InterruptedException {
        client.createTest(test).toCompletableFuture().get();
    }

    private static List<Test> parseMetadata(File file) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject metadataJson = (JSONObject) parser.parse(new FileReader(file));
        String type = (String) metadataJson.get("type");
        if (type == null)
            throw new IllegalArgumentException("metadata must have 'type' field");

        NewmanTestsMetadataParser newmanTestsMetadataParser = NewmanTestsMetadataParserFactory.create(type);
        return newmanTestsMetadataParser.parse(metadataJson);
    }
}
