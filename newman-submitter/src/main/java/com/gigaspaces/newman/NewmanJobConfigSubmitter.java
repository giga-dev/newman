package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.JavaVersion;
import com.gigaspaces.newman.entities.JobConfig;
import com.gigaspaces.newman.utils.EnvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class creates and saves in the DB new job config objects
 *
 * in order to run this please define the static variables in the env variables configuration
 * static variables of the general newman connection and for the specific configuration.
 */

public class NewmanJobConfigSubmitter {

    private static final Logger logger = LoggerFactory.getLogger(NewmanJobConfigSubmitter.class);

    public static final String NEWMAN_CONFIG_NAME = "NEWMAN_CONFIG_NAME";
    public static final String NEWMAN_CONFIG_JAVA_VERSION = "NEWMAN_CONFIG_JAVA_VERSION";


    public static void main(String[] args) throws Exception {

        getAllJobConfigs();

//        addJobConfigToDB();
    }

    private static void getAllJobConfigs() throws Exception {
        NewmanClient newmanClient = NewmanClientUtil.getNewmanClient(logger);
        try {
            List<JobConfig> jobConfigs = newmanClient.getAllConfigs().toCompletableFuture().get(NewmanClientUtil.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.info("jobConfigs from DB: "+jobConfigs);
        }finally {
            newmanClient.close();
        }
    }

    private static void addJobConfigToDB() throws Exception {
        NewmanClient newmanClient = NewmanClientUtil.getNewmanClient(logger);
        try {
            JobConfig jobConfig = new JobConfig();
            jobConfig.setName(EnvUtils.getEnvironment(NEWMAN_CONFIG_NAME, true /*required*/, logger));
            jobConfig.setJavaVersion(JavaVersion.valueOf(EnvUtils.getEnvironment(NEWMAN_CONFIG_JAVA_VERSION, false, logger)));

            logger.info("Adding configuration: " + jobConfig.toString());
            JobConfig result = newmanClient.addConfig(jobConfig).toCompletableFuture().get();
            logger.info("result: " + result);

        } finally {
            newmanClient.close();
        }
    }

}
