package com.gigaspaces.newman;

import com.gigaspaces.newman.utils.FileUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

/**
 * @author Boris
 * @since 1.0
 */
public class NewmanAgentConfig {

    private static final String NEWMAN_HOME = "newman.agent.home";
    private static final String NEWMAN_AGENT_HOST_NAME = "newman.agent.hostname";
    private static final String DEFAULT_NEWMAN_HOME = FileUtils.append(System.getProperty("user.home"), "newman-agent-" + UUID.randomUUID()).toString();
    private static final String NEWMAN_SERVER_HOST = "newman.agent.server-host";
    private static final String DEFAULT_NEWMAN_SERVER_HOST = "localhost";
    private static final String NEWMAN_SERVER_PORT = "newman.agent.server-port";
    private static final String DEFAULT_NEWMAN_SERVER_PORT = "8443";
    private static final String NEWMAN_SERVER_REST_USER = "newman.agent.server-rest-user";
    private static final String DEFAULT_NEWMAN_SERVER_REST_USER = "root";
    private static final String NEWMAN_SERVER_REST_PW = "newman.agent.server-rest-pw";
    private static final String DEFAULT_NEWMAN_SERVER_REST_PW = "root";

    private static final int NUM_OF_WORKERS = Integer.getInteger("newman.agent.workers", 5);
    private static final int JOB_POLL_INTERVAL = Integer.getInteger("newman.agent.job-poll-interval", 1000 * 10);
    private static final int PING_INTERVAL = Integer.getInteger("newman.agent.ping-interval", 1000 * 30);
    private static final boolean PERSISTENT_NAME = Boolean.getBoolean("newman.agent.persistent-name");

    private Properties properties;

    public NewmanAgentConfig() {
        properties = new Properties();
        properties.putIfAbsent(NEWMAN_AGENT_HOST_NAME, loadHostName());
        properties.putIfAbsent(NEWMAN_HOME, getNonEmptySystemProperty(NEWMAN_HOME, DEFAULT_NEWMAN_HOME));
        properties.putIfAbsent(NEWMAN_SERVER_HOST, getNonEmptySystemProperty(NEWMAN_SERVER_HOST, DEFAULT_NEWMAN_SERVER_HOST));
        properties.putIfAbsent(NEWMAN_SERVER_PORT, getNonEmptySystemProperty(NEWMAN_SERVER_PORT, DEFAULT_NEWMAN_SERVER_PORT));
        properties.putIfAbsent(NEWMAN_SERVER_REST_USER, getNonEmptySystemProperty(NEWMAN_SERVER_REST_USER, DEFAULT_NEWMAN_SERVER_REST_USER));
        properties.putIfAbsent(NEWMAN_SERVER_REST_PW, getNonEmptySystemProperty(NEWMAN_SERVER_REST_PW, DEFAULT_NEWMAN_SERVER_REST_PW));
    }

    private String loadHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public boolean isPersistentName() {
        return PERSISTENT_NAME;
    }

    public int getPingInterval() {
        return PING_INTERVAL;
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
