package com.gigaspaces.newman;

import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Created by boris on 4/20/2015.
 */
public class NewmanAgentConfig {

    private static final String NEWMAN_HOME = "newman.agent.home";
    private static final int NUM_OF_WORKERS = Integer.getInteger("newman.agent.workers", 5);
    private static final int JOB_POLL_INTERVAL = Integer.getInteger("newman.agent.job-poll-interval", 2000);
    private static final int WORKER_POLL_INTERVAL = Integer.getInteger("newman.agent.active-workers-poll-interval", 1000);

    private Properties properties;
    private static final Logger logger = LoggerFactory.getLogger(NewmanAgent.class);

    public NewmanAgentConfig(String propsFilePath) {
        properties = new Properties();
        loadPropertiesFile(propsFilePath);
        properties.put("hostName", loadHostName());
        if (properties.getProperty(NEWMAN_HOME) == null)
            properties.put(NEWMAN_HOME, loadNewmanHome());
    }

    private void loadPropertiesFile(String propsFilePath) {
        if (propsFilePath == null || !new File( propsFilePath ).isFile() ) {
            logger.warn("Properties URL is not found [" + propsFilePath + "]. Use default configuration.");
        } else {
            try {
                FileInputStream url = new FileInputStream( new File( propsFilePath ) );
                properties.load(url);
            } catch( IOException ex ) {
                throw new IllegalStateException("Failed to load configuration file.", ex);}
        }
    }

    private String loadHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private String loadNewmanHome() {
        String result = System.getProperty(NEWMAN_HOME);
        if (result == null)
            result = FileUtils.append(System.getProperty("user.home"), "newman-agent").toString();
        return result;
    }

    public String getHostName(){
        return properties.getProperty("hostName");
    }

    public String getNewmanHome() {
        return properties.getProperty(NEWMAN_HOME);
    }

    public int getNumOfWorkers() {
        return NUM_OF_WORKERS;
    }

    public int getJobPollInterval() {
        return JOB_POLL_INTERVAL;
    }
    
    public int getActiveWorkersPollInterval() {
        return WORKER_POLL_INTERVAL;
    }
}
