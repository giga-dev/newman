package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String BUILD_ZIP_FILE_URI = "BUILD_ZIP_FILE_URI";
    private static final String NEWMAN_BUILD_NUMBER = "NEWMAN_BUILD_NUMBER";
    private static final String NEWMAN_BUILD_BRANCH = "NEWMAN_BUILD_BRANCH";
    private static final String TESTS_ZIP_FILE_URI = "TESTS_ZIP_FILE_URI";
    private static final String BUILD_METADATA_FILE_URI = "BUILD_METADATA_FILE_URI";
    private static final String NEWMAN_ARTIFACTS_URI = "NEWMAN_ARTIFACTS_URI";
    private static final String NEWMAN_TGRID_METADATA_URI = "NEWMAN_TGRID_METADATA_URI";


    //0-host, 1- port, 2- user, 3- password
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException, ExecutionException, InterruptedException {
        String buildZipFile = getEnvironment(BUILD_ZIP_FILE_URI);
        String testsZipFile = getEnvironment(TESTS_ZIP_FILE_URI);
        String buildMetadataFile = getEnvironment(BUILD_METADATA_FILE_URI);
        String newmanArtifactsUri = getEnvironment(NEWMAN_ARTIFACTS_URI);
        String newmanTgridMetadataUri = getEnvironment(NEWMAN_TGRID_METADATA_URI);
        String buildNumber = getEnvironment(NEWMAN_BUILD_NUMBER);
        String buildBranch = getEnvironment(NEWMAN_BUILD_BRANCH);

        if (args.length != 4){
            logger.error("Usage: java -cp newman-submitter-1.0-shaded.jar com.gigaspaces.newman.NewmanBuildSubmitter <newmanServerHost> <newmanServerPort> <newmanUser> <newmanPassword>");
            System.exit(1);
        }

        String host = args[0];
        String port = args[1];
        String username = args[2];
        String password = args[3];

        logger.info("connecting to {}:{} with username: {} and password: {}", host, port, username, password);
        NewmanClient newmanClient = NewmanClient.create(host, port, username, password);

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
        testMetadata.add(URI.create(newmanTgridMetadataUri));
        //TODO add sgtest metadata
        b.setTestsMetadata(testMetadata);
        Build build = newmanClient.createBuild(b).toCompletableFuture().get();

        logger.info("Build {} was created successfully", build);
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
        Path file = FileUtils.download(URI.create(buildMetadataFile).toURL(),Paths.get("."));
        String metadata = FileUtils.readTextFile(file);
        String modifiedMetadata = metadata.replace("[", "");
        modifiedMetadata = modifiedMetadata.replace("]", "");
        modifiedMetadata = modifiedMetadata.replace("\"", "");
        modifiedMetadata = modifiedMetadata.replaceAll("\\s+", "");
        String[] splicedMetadata = modifiedMetadata.split(",");
        for (String element : splicedMetadata){
            shas.put(element.split(":")[0], element.split(":")[1]);
        }
        //noinspection ResultOfMethodCallIgnored
        file.toFile().delete();
        return shas;
    }
}
