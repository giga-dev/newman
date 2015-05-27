package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Boris
 * @since 1.0
 * Submits build to newman server by request
 */
public class NewmanBuildSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanBuildSubmitter.class);

    private static final String BUILD_S3_PUBLISH_FOLDER = "BUILD_S3_PUBLISH_FOLDER";
    private static final String NEWMAN_BUILD_MILESTONE = "NEWMAN_BUILD_MILESTONE";
    private static final String NEWMAN_BUILD_VERSION = "NEWMAN_BUILD_VERSION";
    private static final String NEWMAN_BUILD_NUMBER = "NEWMAN_BUILD_NUMBER";
    private static final String NEWMAN_BUILD_BRANCH = "NEWMAN_BUILD_BRANCH";

    private static final String NEWMAN_HOST = "NEWMAN_HOST";
    private static final String NEWMAN_PORT = "NEWMAN_PORT";
    private static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    private static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException {

        // build arguments
        String publishFolder = getEnvironment(BUILD_S3_PUBLISH_FOLDER);
        String newmanBuildMilestone = getEnvironment(NEWMAN_BUILD_MILESTONE);
        String newmanBuildVersion = getEnvironment(NEWMAN_BUILD_VERSION);
        String buildBranch = getEnvironment(NEWMAN_BUILD_BRANCH);
        String buildNumber = getEnvironment(NEWMAN_BUILD_NUMBER);

        // connection arguments
        String host = getEnvironment(NEWMAN_HOST);
        String port = getEnvironment(NEWMAN_PORT);
        String username = getEnvironment(NEWMAN_USER_NAME);
        String password = getEnvironment(NEWMAN_PASSWORD);

        String buildPathPrefix = "http://tarzan/builds/GigaSpacesBuilds/";
        String baseBuildURI = buildPathPrefix + newmanBuildVersion + "/build_" + buildNumber;
        String buildZipFile = baseBuildURI +"/xap-premium/1.5/gigaspaces-xap-premium-" + newmanBuildVersion + "-" +newmanBuildMilestone + "-b" + buildNumber +".zip";
        String testsZipFile = baseBuildURI + "/testsuite-1.5.zip";
        String buildMetadataFile = baseBuildURI + "/xap-premium/1.5/metadata.txt";
        String newmanArtifactsUri = "https://s3-eu-west-1.amazonaws.com/gigaspaces-repository-eu/com/gigaspaces/xap-core/newman/"+ publishFolder +"/newman-artifacts.zip";
        String newmanTgridMetadataUri = "jar:" + testsZipFile +"!/QA/metadata/tgrid-tests-metadata.json";

        logger.info("Initialized newman build submitter with the following arguments:");
        logger.info("\nbuildZipFile={}\ntestsZipFile={}\nbuildMetadataFile={}\nnewmanArtifactsUri={}\nnewmanTgridMetadataUri={}",
                buildZipFile, testsZipFile, buildMetadataFile, newmanArtifactsUri, newmanTgridMetadataUri);

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        NewmanClient newmanClient = NewmanClient.create(host, port, username, password);
        try {
            Build b = new Build();
            b.setName(buildNumber);
            b.setBranch(buildBranch);
            Map<String, String> shas = parseBuildMetadata(buildMetadataFile);
            b.setShas(shas);
            URI artifactsURI = URI.create(newmanArtifactsUri);
            URI testsURI = URI.create(testsZipFile);
            URI buildURI = URI.create(buildZipFile);

            Collection<URI> collection = new ArrayList<>();
            collection.add(artifactsURI);
            collection.add(testsURI);
            collection.add(buildURI);
            b.setResources(collection);
            Collection<URI> testMetadata = new ArrayList<>();
            URI tgridMetadata = URI.create(newmanTgridMetadataUri);
            testMetadata.add(tgridMetadata);
            //TODO add sgtest metadata
            b.setTestsMetadata(testMetadata);
            Build build = newmanClient.createBuild(b).toCompletableFuture().get();

            logger.info("Build {} was created successfully", build);
        } finally {
            newmanClient.close();
        }
    }

    private static String getEnvironment(String var) {
        String v = System.getenv(var);
        if (v == null){
            logger.error("Please set the environment variable {} and try again.", var);
            throw new IllegalArgumentException("the environment variable " + var + " must be set");
        }
        return v;
    }

    private static Map<String, String> parseBuildMetadata(String buildMetadataFile) throws IOException {
        Map<String,String> shas = new HashMap<>();
        InputStream is = null;
        try {
            is = URI.create(buildMetadataFile).toURL().openStream();
            String metadata = FileUtils.readTextFile(is);
            String modifiedMetadata = metadata.replace("[", "");
            modifiedMetadata = modifiedMetadata.replace("]", "");
            modifiedMetadata = modifiedMetadata.replace("\"", "");
            modifiedMetadata = modifiedMetadata.replaceAll("\\s+", "");
            String[] splicedMetadata = modifiedMetadata.split(",");
            for (String element : splicedMetadata) {
                shas.put(element.split(":")[0], element.split(":")[1]);
            }
        }
        finally {
            if (is != null) {
                is.close();
            }
        }
        return shas;
    }
}
