package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.utils.EnvUtils;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
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

    private NewmanClient newmanClient;
    private NewmanBuildMetadata buildMetadata;

    public NewmanBuildSubmitter(String host, String port, String username, String password) {

        this.buildMetadata = getBuildMetadata();

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        try {
            newmanClient = NewmanClient.create(host, port, username, password);
        } catch (Exception e) {
            logger.error("Failed to init client, exiting...", e);
            System.exit(1);
        }
    }

    private NewmanBuildMetadata getBuildMetadata() {
        NewmanBuildMetadata buildMetadata = new NewmanBuildMetadata();
        //e.g "13507-106"
        String buildNumber = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_NUMBER, logger);
        buildMetadata.setBuildNumber(buildNumber);

        //e.g "master"
        String buildBranch = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_BRANCH, logger);
        buildMetadata.setBuildBranch(buildBranch);

        // e.g QB,DOTNET
        String newmanBuildTags = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_TAGS, false, logger);
        if (newmanBuildTags != null) {
            Set<String> tags = new HashSet<>();
            Collections.addAll(tags, newmanBuildTags.split(","));
            buildMetadata.setTags(tags);
        }

        //e.g "http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/xap-premium/1.5/metadata.txt";
        String newmanBuildShasFile = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_SHAS_FILE, logger);
        buildMetadata.setBuildShasFile(newmanBuildShasFile);

        //e.g "http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/testsuite-1.5.zip,
        //      http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/xap-premium/1.5/gigaspaces-xap-premium-10.2.0-ga-b13507-106.zip,
        //      https://s3-eu-west-1.amazonaws.com/gigaspaces-repository-eu/com/gigaspaces/xap-core/newman/10.2.0-13507-106-SNAPSHOT/newman-artifacts.zip"
        String newmanBuildResources = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_RESOURCES, logger);
        Collection<String> resources = new ArrayList<>();
        Collections.addAll(resources, newmanBuildResources.split(","));
        buildMetadata.setResources(resources);

        //e.g "jar:http://tarzan/builds/GigaSpacesBuilds/10.2.0/build_13507-106/testsuite-1.5.zip!/QA/metadata/tgrid-tests-metadata.json"
        String newmanBuildTestsMetadata = EnvUtils.getEnvironment(NewmanBuildMetadata.NEWMAN_BUILD_TESTS_METADATA, logger);
        Collection<String> testsMetadata = new ArrayList<>();
        Collections.addAll(testsMetadata, newmanBuildTestsMetadata.split(","));
        buildMetadata.setTestsMetadata(testsMetadata);

        return buildMetadata;
    }

    public String submitBuild() throws IOException, ExecutionException, InterruptedException {

        logger.info("Initialized newman build submitter with the following build metadata: " + buildMetadata);
        try {
            Build b = new Build();
            b.setName(buildMetadata.getBuildNumber());
            b.setBranch(buildMetadata.getBuildBranch());
            Map<String, String> shas = parseBuildShasFile(buildMetadata.getBuildShasFile());
            b.setShas(shas);
            // set resources
            Collection<URI> buildResources = buildMetadata.getResources().stream().map(URI::create).collect(Collectors.toList());
            b.setResources(buildResources);
            // set tests metadata
            Collection<URI> testsMetadata = buildMetadata.getTestsMetadata().stream().map(URI::create).collect(Collectors.toList());
            b.setTestsMetadata(testsMetadata);
            // set tags
            b.setTags(buildMetadata.getTags());
            Build build = newmanClient.createBuild(b).toCompletableFuture().get();
            logger.info("Build {} was created successfully", build);
            return build.getId();

        } finally {
            newmanClient.close();
        }
    }

    private Map<String, String> parseBuildShasFile(String buildShasFile) throws IOException {
        Map<String,String> shas = new HashMap<>();
        InputStream is = null;
        try {
            is = URI.create(buildShasFile).toURL().openStream();
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

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException {
        // connection arguments
        String host = EnvUtils.getEnvironment(NEWMAN_HOST, logger);
        String port = EnvUtils.getEnvironment(NEWMAN_PORT, logger);
        String username = EnvUtils.getEnvironment(NEWMAN_USER_NAME, logger);
        String password = EnvUtils.getEnvironment(NEWMAN_PASSWORD, logger);

        NewmanBuildSubmitter buildSubmitter = new NewmanBuildSubmitter(host, port, username, password);

        buildSubmitter.submitBuild();
    }

}
