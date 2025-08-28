package com.gigaspaces.newman;

import com.gigaspaces.newman.entities.Job;
import com.gigaspaces.newman.entities.Suite;
import com.gigaspaces.newman.entities.Test;
import com.gigaspaces.newman.utils.FileUtils;
import com.gigaspaces.newman.utils.ProcessResult;
import com.gigaspaces.newman.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.gigaspaces.newman.utils.FileUtils.*;
import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

public class JobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JobExecutor.class);
    private static String SCRIPT_SUFFIX = isWindows() ? ".bat" : ".sh";

    private final Job job;
    private final Path jobFolder;
    private final Path newmanLogFolder;
    private final String proxy = getNonEmptySystemProperty("resources.proxy.url", null);

    public JobExecutor(Job job, String basePath) {
        this.job = job;
        this.newmanLogFolder = append(Paths.get(basePath), "logs");
        this.jobFolder = append(basePath, "job-" + job.getId()+ "-" + UUID.randomUUID()); // generate unique job folder for each execution
    }

    public Job getJob() {
        return job;
    }

    public boolean setup() {
        logger.info("Starting setup for job {}...", job.getId());
        try {
            logger.info("Creating job folder {}", jobFolder);
            createFolder(jobFolder);

            Path resourcesFolder = createFolder(append(jobFolder, "resources"));
            if (job.getBuild().getResources() == null){
                logger.warn("The job {} has no resources", job.getId());
                return false;
            }
            logger.info("Downloading {} resources into {}...", job.getBuild().getResources().size(), resourcesFolder);
            validateUris(job.getBuild().getResources());
            for (URI resource : job.getBuild().getResources()) {
                URI proxiedResource = wrapWithProxy(resource);
                logger.info("Downloading {}...", proxiedResource);
                download(proxiedResource.toURL(), resourcesFolder);
            }

            logger.info("Extracting Newman Artifacts...");
            Path artifactsFile = append(resourcesFolder, "newman-artifacts.zip");
            unzip(artifactsFile, jobFolder);

            //chmod in linux to execute scripts
            if (!isWindows()){
                String[] scriptExtensions = {"sh"};
                for(Object script : FileUtils.listFilesInFolder(jobFolder.toFile(), scriptExtensions)){
                    boolean chmodSuccess = ((File) script).setExecutable(true);
                    if (!chmodSuccess)
                        logger.warn("failed to chmod the script file: {}" + script);
                }
            }

            logger.info("Executing setup script...");
            Path setupScript = append(jobFolder, "job-setup" + SCRIPT_SUFFIX);
            ProcessResult result = ProcessUtils.executeAndWait(setupScript, Collections.emptyList(), jobFolder,
                    append(jobFolder, "job-setup.log"), getCustomVariablesFromSuite(), overrideSetupTimeoutIfRequested(), true);
            if (result.getExitCode() == null || result.getExitCode() != 0)
                throw new IOException("Setup script " + ((result.getExitCode() == null) ? "timed out" : "returned ") + result.getExitCode());

            logger.info("Setup for job {} completed successfully", job.getId());
            return true;
        } catch (IOException e) {
            logger.error("Setup for job {} has failed: {}", job.getId(), e);
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            logger.error("Setup for job {} was interrupted", job.getId());
            e.printStackTrace();
            return false;
        }
    }

    private URI wrapWithProxy(URI origPath) {
        if (proxy == null) {
            logger.info("No proxy has been found");
            return origPath;
        }

        logger.info("Using proxy - {}", proxy);
        String origPathStr = proxy.split(":")[0];
        String proxyPathStr = proxy.split(":")[1];

        try {
            return new URI(origPath.toString().replace(origPathStr, proxyPathStr));
        } catch (URISyntaxException e) {
            logger.error("Failed to wrap proxy URI {} with {}: {}", origPathStr, proxyPathStr, e);
            return origPath;
        }
    }

    private long overrideSetupTimeoutIfRequested() {
        Map<String, String> customVariablesFromSuite = getCustomVariablesFromSuite();
        String customTimeout = customVariablesFromSuite.get(Suite.CUSTOM_SETUP_TIMEOUT);
        return customTimeout != null ? Long.parseLong(customTimeout) : ProcessUtils.DEFAULT_SCRIPT_TIMEOUT;
    }

    public Test run(Test test) {
        logger.info("Starting test {}...", test.getName());
        final Path testFolder = append(jobFolder, "test-" + test.getId());
        final Path outputFolder = append(testFolder, "output");

        try {
            logger.info("Creating test folder - {}", testFolder);
            createFolder(testFolder);
            createFolder(outputFolder);

            logger.info("Starting test script");
            Path testScript = append(jobFolder, "run-" + test.getTestType() + SCRIPT_SUFFIX);
            Path outputFile = append(outputFolder, "runner-output.log");
            ProcessResult scriptResult = ProcessUtils.executeAndWait(testScript, wrapUniqueArgsWithQuotes(test.getArguments()), testFolder,
                    outputFile, getCustomVariablesFromSuite(), test.getTimeout().longValue());

            // Generate result:
            test.setStartTime(new Date(scriptResult.getStartTime()));
            test.setEndTime(new Date(scriptResult.getEndTime()));
            if (scriptResult.getExitCode() != null) {
                test.setStatus(scriptResult.getExitCode() == 0 ? Test.Status.SUCCESS : Test.Status.FAIL);
                //set error message when test failed
                if (test.getStatus() == Test.Status.FAIL){
                    try {
                        String errorMessage = readTextFile(append(outputFolder, "error.txt"));
                        test.setErrorMessage(errorMessage);
                    }
                    catch (IOException e){
                        test.setErrorMessage("No error.txt file");
                    }
                }
            } else {
                // test timed out
                test.setStatus(Test.Status.FAIL);
                test.setErrorMessage("Test exceeded timeout - " + test.getTimeout() + "ms");
            }
            // call teardown test if exists
            teardownTest(test);
        }
        catch (Throwable t) {
            test.setStatus(Test.Status.FAIL);
            test.setErrorMessage("Newman caught " + t + " while executing test");
        }

        // Pack & upload output files (logs, etc.)
        try {
            //noinspection unchecked
            zip(FileUtils.listFilesInFolder(outputFolder.toFile()), append(testFolder, "output.zip"));
        } catch (IOException e) {
            logger.warn("Failed to zip output folder: " + outputFolder, e);
        }
        // TODO: Where should the output.zip be uploaded to? synchronously?
        try {
            FileUtils.copyFile(append(testFolder, "output.zip"), append(newmanLogFolder , "output-" + test.getId() + ".zip"));
        } catch (IOException e) {
            logger.warn("Failed to upload output zip", e);
        }

        // Cleanup:
        try {
            delete(testFolder);
        } catch (IOException e) {
            logger.warn("Failed to delete test folder {}", testFolder);
        }

        return test; //return same reference
    }

    private boolean shouldBeWrapped(String arg) {
        return !(arg.startsWith("\"") && (arg.endsWith("\""))) && (arg.contains("&") || arg.contains("=") || arg.contains(","));
    }

    private Collection<String> wrapUniqueArgsWithQuotes(List<String> arguments) {
        if (isWindows()){
            for (int i=0; i<arguments.size(); i++) {
                if (shouldBeWrapped(arguments.get(i))){
                    arguments.set(i, "\"" + arguments.get(i) + "\"".replaceAll("\\s+",""));
                }
            }
        }
        return arguments;
    }

    private void teardownTest(Test test) {
        logger.info("Tearing down test {}", test.getName());

        Path testFolder = append(jobFolder, "test-" + test.getId());
        Path outputFile = append(append(testFolder, "output"), "end-" + test.getTestType() + ".log");
        Path teardownScript = append(jobFolder, "end-" + test.getTestType() + SCRIPT_SUFFIX);

        logger.info("testFolder path: {}", testFolder);
        logger.info("outputFile path: {}", outputFile);
        logger.info("teardownScript path: {}", teardownScript);

        if (exists(teardownScript)) {
            logger.info("Executing teardown for test {}", test);
            try {
                ProcessUtils.executeAndWait(teardownScript, wrapUniqueArgsWithQuotes(test.getArguments()),testFolder,
                         outputFile, getCustomVariablesFromSuite(), 10 * 60 * 1000);
                // TODO: Inspect exit code and log warning if non-zero.
            } catch (Exception e) {
                logger.warn("failed to teardown test" + test, e);
            }
        }
    }

    private Map<String, String> getCustomVariablesFromSuite() {
        String customVariableString = job.getSuite() != null ? job.getSuite().getCustomVariables() : null;
        Map<String, String> customVariables = Suite.parseCustomVariables(customVariableString);
        injectBuildBranch(customVariables);

        //Added job configuration to the custom variables to be passed to the xap
        customVariables.put("JAVA_VERSION",job.getJobConfig().getJavaVersion().getName());

        return customVariables;
    }

    private void injectBuildBranch(Map<String, String> customVariables) {
        if (job.getBuild() != null && job.getBuild().getBranch() != null) {
            customVariables.putIfAbsent("GIT_BRANCH", job.getBuild().getBranch());
        }
    }

    public void teardown() {
        logger.info("Starting teardown for job {}...", job.getId());
        try {
            // Execute teardown script (if exists):
            Path teardownScript = append(jobFolder, "job-teardown" + SCRIPT_SUFFIX);
            if (exists(teardownScript)) {
                logger.info("Executing teardown script...");
                ProcessUtils.executeAndWait(teardownScript, jobFolder, append(jobFolder, "job-teardown.log"), getCustomVariablesFromSuite());
                // TODO: Inspect exit code and log warning if non-zero.
            }
            logger.info("Deleting job folder {}", jobFolder);
            delete(jobFolder);
            clearDirectory(newmanLogFolder);
        } catch (IOException e) {
            logger.error("Teardown for job {} has failed: {}", job.getId(), e);
        } catch (InterruptedException e) {
            logger.error("Teardown for job {} was interrupted", job.getId());
        }
    }

    public Path getJobFolder(){
        return this.jobFolder;
    }
}
