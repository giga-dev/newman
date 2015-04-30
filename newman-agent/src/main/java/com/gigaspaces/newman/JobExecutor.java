package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.utils.ProcessResult;
import com.gigaspaces.newman.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Date;

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
            for (URI resource : job.getBuild().getResources()) {
                logger.info("Downloading {}...", resource);
                download(resource.toURL(), resourcesFolder);
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
            return true;
        } catch (IOException e) {
            logger.error("Setup for job {} has failed: {}", job.getId(), e);
            return false;
        } catch (InterruptedException e) {
            logger.error("Setup for job {} was interrupted", job.getId());
            return false;
        }
    }

    public Test run(Test test) {
        logger.info("Starting test {}...", test.getLocalId());
        final Path testFolder = append(jobFolder, "test-" + test.getLocalId());
        final Path outputFolder = append(testFolder, "output");

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
            test.setStartTime(new Date(scriptResult.getStartTime()));
            test.setEndTime(new Date(scriptResult.getEndTime()));
            if (scriptResult.getExitCode() != null) {
                test.setStatus(scriptResult.getExitCode() == 0 ? Test.Status.SUCCESS : Test.Status.FAIL);
                try {
                    String errorMessage = readTextFile(append(testFolder, "error.txt"));
                    test.setErrorMessage(errorMessage);
                }
                catch (IOException e){
                    test.setErrorMessage("No error.txt file");
                }
            } else {
                // test timed out
                test.setStatus(Test.Status.FAIL);
                test.setErrorMessage("Test exceeded timeout - " + test.getTimeout() + "ms");
            }
        }
        catch (Throwable t) {
            test.setStatus(Test.Status.FAIL);
            test.setErrorMessage("Newman caught " + t + " while executing test");
        }

        // Pack & upload output files (logs, etc.)
        try {
            zip(outputFolder, append(testFolder, "output.zip"));
        } catch (IOException e) {
            logger.warn("Failed to zip output folder folder {}", outputFolder);
        }
        // TODO: Where should the output.zip be uploaded to? synchronously?
        /*try {
            FileUtils.copyFile(append(testFolder, "output.zip"), append(append(newmanFolder, "logs"), "output" + test.getLocalId() + ".zip"));
        } catch (IOException e) {
            logger.warn("Failed to upload output zip");
        }*/

        // Cleanup:
        try {
            delete(testFolder);
        } catch (IOException e) {
            logger.warn("Failed to delete test folder {}", testFolder);
        }

        return test; //return same reference
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
