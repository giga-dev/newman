package com.gigaspaces.newman.crons.cleanse;

import com.gigaspaces.newman.NewmanClient;
import com.gigaspaces.newman.analytics.CronJob;
import com.gigaspaces.newman.server.NewmanServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by tamirt
 * on 8/25/15.
 */
public class CleanseCronJob implements CronJob {
    private static final Logger logger = LoggerFactory.getLogger(CleanseCronJob.class);
    public static final String CONS_CLEANSE_SIZE_PERCENT = "crons.cleanse.sizePercent";
    public static final String CONS_CLEANSE_NUMBER_OF_JOBS = "crons.cleanse.numberOfJobs";
    public static final String CONS_CLEANSE_DISK_PARTITION = "crons.cleanse.diskPartition";

    public void run(Properties properties) {
        NewmanServerConfig config = new NewmanServerConfig();
        NewmanClient newmanClient = null;
        try {
            newmanClient = NewmanClient.create(config.getNewmanServerHost(), config.getNewmanServerPort(),
                    config.getNewmanServerRestUser(), config.getNewmanServerRestPassword());
            cleanse(newmanClient, properties);
        } catch (Exception e) {
            logger.warn(e.toString(), e);
        } finally {
            if (newmanClient != null) {
                newmanClient.close();
            }
        }

    }

    private void cleanse(NewmanClient newmanClient, Properties properties) throws Exception {

        final String requiredFreeDiskSpacePercentage = properties.getProperty(CONS_CLEANSE_SIZE_PERCENT);
        final String numberOfJobs = properties.getProperty(CONS_CLEANSE_NUMBER_OF_JOBS);
        final String diskPartition=properties.getProperty(CONS_CLEANSE_DISK_PARTITION);
        long start = System.currentTimeMillis();
        Integer numOfJobsDeleted = newmanClient.deleteJobUntilDesiredSpace(requiredFreeDiskSpacePercentage, numberOfJobs,diskPartition).toCompletableFuture().get(10, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();

        logger.info("Deleted " + numOfJobsDeleted + " jobs, took: " + (end-start) + " ms");
    }

}
