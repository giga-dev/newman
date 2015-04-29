package com.gigaspaces.newman.jobexecutor;

import com.gigaspaces.newman.JobExecutor;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.utils.MockUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.gigaspaces.newman.utils.FileUtils.*;


public class JobExecutorTests {

    private static String SCRIPT_SUFFIX = isWindows() ? ".bat" : ".sh";
    private JobExecutor je;
    Path basePath;
    Job job;

    @Before
    public void before(){
        job = MockUtils.createMockJob();
        basePath = append(System.getProperty("user.home"), "newman-agent-test");
        je = new JobExecutor(job, basePath.toString());
    }

    @Test
    public void testSetup(){
        Assert.assertNotNull(job);
        Path jobFolder = append(basePath, "job-" + job.getId());
        Assert.assertTrue(je.setup());
        assertExists(jobFolder);
        Path resourcesFolder = (append(jobFolder, "resources"));
        assertExists(resourcesFolder);
        Path newmanArtifacts = (append(resourcesFolder, "newman-artifacts.zip"));
        assertExists(newmanArtifacts);
        Path setupScript = append(jobFolder, "job-setup" + SCRIPT_SUFFIX);
        assertExists(setupScript);
    }

    @After
    public void after(){
        je.teardown();
        try {
            delete(basePath);
        } catch (IOException e) {}
    }

    private void assertExists(Path file) {
        Assert.assertTrue("File " + file + " does not exists", exists(file));
    }
}
