package com.gigaspaces.newman;

import com.gigaspaces.newman.entities.Build;
import com.gigaspaces.newman.utils.EnvUtils;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author Boris
 * @since 1.0
 * Submits build to newman server by request
 */
public class NewmanBuildSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanBuildSubmitter.class);

    private static final String NEWMAN_HOST = "NEWMAN_HOST";
    private static final String NEWMAN_PORT = "NEWMAN_PORT";
    private static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    private static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";
    private static final String NEWMAN_APPEND_TO_BUILD = "NEWMAN_APPEND_TO_BUILD";

    private NewmanClient newmanClient;
    private NewmanBuildMetadata buildMetadata;

    public NewmanBuildSubmitter(String host, String port, String username, String password) {

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    public void initBuildMetadata(boolean appendToBuild) {
        this.buildMetadata = new NewmanBuildMetadata();
        //e.g "13507-106"
        String buildNumber = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_NUMBER, logger);
        buildMetadata.setBuildNumber(buildNumber);

        //e.g "master"
        String buildBranch = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_BRANCH, !appendToBuild, logger);
        buildMetadata.setBuildBranch(buildBranch);

        // e.g QB,DOTNET
        String newmanBuildTags = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_TAGS, false, logger);
        if (newmanBuildTags != null) {
            Set<String> tags = new HashSet<>();
            Collections.addAll(tags, newmanBuildTags.split(","));
            buildMetadata.setTags(tags);
        }

        //e.g "http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/xap-premium/1.5/metadata.txt";
        String newmanBuildShasFile = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_SHAS_FILE, !appendToBuild, logger);
        if (newmanBuildShasFile != null) {
            buildMetadata.setBuildShasFile(newmanBuildShasFile);
        }

        //e.g "http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/testsuite-1.5.zip,
        //      http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/xap-premium/1.5/gigaspaces-xap-premium-10.2.0-ga-b13507-106.zip,
        //      https://s3-eu-west-1.amazonaws.com/gigaspaces-repository-eu/com/gigaspaces/xap-core/newman/10.2.0-13507-106-SNAPSHOT/newman-artifacts.zip"
        String newmanBuildResources = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_RESOURCES, !appendToBuild, logger);
        if (newmanBuildResources != null) {
            Collection<String> resources = new ArrayList<>();
            Collections.addAll(resources, newmanBuildResources.split(","));
            buildMetadata.setResources(resources);
        }

        //e.g "jar:http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/testsuite-1.5.zip!/QA/metadata/tgrid-tests-metadata.json"
        String newmanBuildTestsMetadata = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_TESTS_METADATA, !appendToBuild, logger);
        if (newmanBuildTestsMetadata != null) {
            Collection<String> testsMetadata = new ArrayList<>();
            Collections.addAll(testsMetadata, newmanBuildTestsMetadata.split(","));
            buildMetadata.setTestsMetadata(testsMetadata);
        }
    }

    public String submitBuild(boolean appendToBuild) throws IOException, ExecutionException, InterruptedException, TimeoutException {

        logger.info("Initialized newman build submitter with the following build metadata: " + buildMetadata);
        try {
            Build b = new Build();
            b.setName(buildMetadata.getBuildNumber());

            if (buildMetadata.getBuildBranch() != null) {
                b.setBranch(buildMetadata.getBuildBranch());
            }

            if (buildMetadata.getBuildShasFile() != null) {
                Map<String, String> shas = parseBuildShasFile(buildMetadata.getBuildShasFile());
                b.setShas(shas);
            }
            // set resources
            if (buildMetadata.getResources() != null) {
                Collection<URI> buildResources = buildMetadata.getResources().stream().map(URI::create).collect(Collectors.toList());
                FileUtils.validateUris(buildResources);
                b.setResources(buildResources);
            }
            // set tests metadata
            if (buildMetadata.getTestsMetadata() != null) {
                Collection<URI> testsMetadata = buildMetadata.getTestsMetadata().stream().map(URI::create).collect(Collectors.toList());
                FileUtils.validateUris(testsMetadata);
                b.setTestsMetadata(testsMetadata);
            }
            // set tags
            if (buildMetadata.getTags() != null) {
                b.setTags(buildMetadata.getTags());
            }
            Build build;

            if (!appendToBuild) {
                build = newmanClient.createBuild(b).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            else {
                build = newmanClient.appendToBuild(b).toCompletableFuture().get(NewmanSubmitter.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            logger.info("Build {} was created successfully", build);
            return build.getId();

        }catch (Exception e){
            logger.error("can't submit build. exception: {}", e);
            throw e;
        }
        finally {
            newmanClient.close();
        }
    }

    private Map parseBuildShasFile( String buildShasFile ) throws IOException {

        Properties properties = new Properties();
        InputStream input = URI.create(buildShasFile).toURL().openStream();
        //load a shas properties file
        properties.load( input );

        return properties;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException, TimeoutException {
        // connection arguments
        String host = EnvUtils.getEnvironment(NEWMAN_HOST, logger);
        String port = EnvUtils.getEnvironment(NEWMAN_PORT, logger);
        String username = EnvUtils.getEnvironment(NEWMAN_USER_NAME, logger);
        String password = EnvUtils.getEnvironment(NEWMAN_PASSWORD, logger);

        boolean append = Boolean.parseBoolean(EnvUtils.getEnvironment(NEWMAN_APPEND_TO_BUILD, false, logger));
        NewmanBuildSubmitter buildSubmitter = new NewmanBuildSubmitter(host, port, username, password);
        buildSubmitter.initBuildMetadata(append);
        buildSubmitter.submitBuild(append);
    }
}