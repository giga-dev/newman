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

import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

/**
 * Created by boris on 4/20/2015.
 */
public class NewmanAgentConfig {

    private static final String NEWMAN_HOME = "newman.agent.home";
    private static final String NEWMAN_AGENT_HOST_NAME = "newman.agent.hostname";
    private static final String DEFAULT_NEWMAN_HOME = FileUtils.append(System.getProperty("user.home"), "newman-agent").toString();
    private static final String NEWMAN_SERVER_HOST = "newman.agent.server-host";
    private static final String DEFAULT_NEWMAN_SERVER_HOST = "localhost";
    private static final String NEWMAN_SERVER_PORT = "newman.agent.server-port";
    private static final String DEFAULT_NEWMAN_SERVER_PORT = "8443";
    private static final String NEWMAN_SERVER_REST_USER = "newman.agent.server-rest-user";
    private static final String DEFAULT_NEWMAN_SERVER_REST_USER = "root";
    private static final String NEWMAN_SERVER_REST_PW = "newman.agent.server-rest-pw";
    private static final String DEFAULT_NEWMAN_SERVER_REST_PW = "root";

    private static final int NUM_OF_WORKERS = Integer.getInteger("newman.agent.workers", 5);
    private static final int JOB_POLL_INTERVAL = Integer.getInteger("newman.agent.job-poll-interval", 2000);

    private Properties properties;
    private static final Logger logger = LoggerFactory.getLogger(NewmanAgent.class);

    public NewmanAgentConfig(String propsFilePath) {
        properties = new Properties();
        loadPropertiesFile(propsFilePath);
        properties.putIfAbsent(NEWMAN_AGENT_HOST_NAME, loadHostName());
        properties.putIfAbsent(NEWMAN_HOME, getNonEmptySystemProperty(NEWMAN_HOME, DEFAULT_NEWMAN_HOME));
        properties.putIfAbsent(NEWMAN_SERVER_HOST, getNonEmptySystemProperty(NEWMAN_SERVER_HOST, DEFAULT_NEWMAN_SERVER_HOST));
        properties.putIfAbsent(NEWMAN_SERVER_PORT, getNonEmptySystemProperty(NEWMAN_SERVER_PORT, DEFAULT_NEWMAN_SERVER_PORT));
        properties.putIfAbsent(NEWMAN_SERVER_REST_USER, getNonEmptySystemProperty(NEWMAN_SERVER_REST_USER, DEFAULT_NEWMAN_SERVER_REST_USER));
        properties.putIfAbsent(NEWMAN_SERVER_REST_PW, getNonEmptySystemProperty(NEWMAN_SERVER_REST_PW, DEFAULT_NEWMAN_SERVER_REST_PW));
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

    public String getHostName(){
        return properties.getProperty(NEWMAN_AGENT_HOST_NAME);
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

    public String getNewmanServerHost() {
        return properties.getProperty(NEWMAN_SERVER_HOST);
    }

    public String getNewmanServerPort() {
        return properties.getProperty(NEWMAN_SERVER_PORT);
    }

    public String getNewmanServerRestUser() {
        return properties.getProperty(NEWMAN_SERVER_REST_USER);
    }

    public String getNewmanServerRestPw() {
        return properties.getProperty(NEWMAN_SERVER_REST_PW);
    }

    public Properties getProperties() {
        return properties;
    }
}
