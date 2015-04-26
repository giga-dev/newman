package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.TestResult;
import com.gigaspaces.newman.utils.ProcessResult;
import com.gigaspaces.newman.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static com.gigaspaces.newman.utils.FileUtils.*;

public class JobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JobExecutor.class);
    private static String SCRIPT_SUFFIX = isWindows() ? ".bat" : ".sh";

    private final Job job;
    private final Path jobFolder;

    public JobExecutor(Job job, String basePath) {
        this.job = job;
        this.jobFolder = append(basePath, "job-" + job.getId());
    }

    public Job getJob() {
        return job;
    }

    public void setup() {
        logger.info("Starting setup for job {}...", job.getId());
        try {
            logger.info("Creating job folder {}", jobFolder);
            createFolder(jobFolder);

            Path resourcesFolder = createFolder(append(jobFolder, "resources"));
            logger.info("Downloading {} resources into {}...", job.getResources().size(), resourcesFolder);
            for (String resource : job.getResources()) {
                logger.info("Downloading {}...", resource);
                download(new URL(resource), resourcesFolder);
            }

            logger.info("Extracting Newman Artifacts...");
            Path artifactsFile = append(resourcesFolder, "newman-artifacts.zip");
            unzip(artifactsFile, jobFolder);

            logger.info("Executing setup script...");
            Path setupScript = append(jobFolder, "job-setup" + SCRIPT_SUFFIX);
            ProcessResult result = ProcessUtils.executeAndWait(setupScript, jobFolder, append(jobFolder, "job-setup.log"));
            if (result.getExitCode() != 0)
                throw new IOException("Setup script " + result.getExitCode() == null ? "timed out" : "returned " + result.getExitCode());

            logger.info("Setup for job {} completed successfully", job.getId());
        } catch (IOException e) {
            logger.error("Setup for job {} has failed: {}", job.getId(), e);
        } catch (InterruptedException e) {
            logger.error("Setup for job {} was interrupted", job.getId());
        }
    }

    public TestResult run(Test test) {
        logger.info("Starting test {}...", test.getLocalId());
        final Path testFolder = append(jobFolder, "test-" + test.getLocalId());
        final Path outputFolder = append(testFolder, "output");
        final TestResult testResult = new TestResult();
        testResult.setTestId(test.getLocalId());

        try {
            logger.info("Creating test folder - {}", testFolder);
            createFolder(testFolder);
            createFolder(outputFolder);

            logger.info("Starting test script");
            Path testScript = append(jobFolder, "run-" + test.getTestType() + SCRIPT_SUFFIX);
            Path outputFile = append(outputFolder, "runner-output.log");
            ProcessResult scriptResult = ProcessUtils.executeAndWait(testScript, test.getArguments(), testFolder,
                    outputFile, test.getTimeout());

            // Generate result:
            testResult.setStartTime(scriptResult.getStartTime());
            testResult.setEndTime(scriptResult.getEndTime());
            if (scriptResult.getExitCode() != null) {
                testResult.setPassed(scriptResult.getExitCode() == 0);
                testResult.setErrorMessage(readTextFile(append(testFolder, "error.txt")));
            } else {
                // test timed out
                testResult.setPassed(false);
                testResult.setErrorMessage("Test exceeded timeout - " + test.getTimeout() + "ms");
            }
        } catch (IOException e) {
            testResult.setPassed(false);
            testResult.setErrorMessage("Newman failed to execute test: " + e);
        } catch (InterruptedException e) {
            testResult.setPassed(false);
            testResult.setErrorMessage("Newman was interrupted while executing test");
        }

        // Pack & upload output files (logs, etc.)
        zip(outputFolder, append(testFolder, "output.zip"));
        // TODO: Where should the output.zip be uploaded to? synchronously?

        // Cleanup:
        try {
            delete(testFolder);
        } catch (IOException e) {
            logger.warn("Failed to delete test folder {}", testFolder);
        }

        return testResult;
    }

    public void teardown() {
        logger.info("Starting teardown for job {}...", job.getId());
        try {
            // Execute teardown script (if exists):
            Path teardownScript = append(jobFolder, "job-teardown" + SCRIPT_SUFFIX);
            if (exists(teardownScript)) {
                logger.info("Executing teardown script...");
                ProcessUtils.executeAndWait(teardownScript, jobFolder, append(jobFolder, "job-teardown.log"));
                // TODO: Inspect exit code and log warning if non-zero.
            }
            logger.info("Deleting job folder {}", jobFolder);
            delete(jobFolder);
        } catch (IOException e) {
            logger.error("Teardown for job {} has failed: {}", job.getId(), e);
        } catch (InterruptedException e) {
            logger.error("Teardown for job {} was interrupted", job.getId());
        }
    }
}
