package com.gigaspaces.newman;

import com.gigaspaces.newman.utils.FileUtils;
import com.gigaspaces.newman.utils.ProcessResult;
import com.gigaspaces.newman.utils.ProcessUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
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
    private static final String NEWMAN_AGENT_HOST_ADDRESS = "newman.agent.address";
    private static final String NEWMAN_AGENT_GROUPNAME = "newman.agent.groupName";
    private static final String DEFAULT_NEWMAN_HOME = FileUtils.append(System.getProperty("user.home"), "newman-agent-" + UUID.randomUUID()).toString();
    private static final String NEWMAN_SERVER_HOST = "newman.agent.server-host";
    private static final String DEFAULT_NEWMAN_SERVER_HOST = "localhost";
    private static final String NEWMAN_SERVER_PORT = "newman.agent.server-port";
    private static final String DEFAULT_NEWMAN_SERVER_PORT = "8443";
    private static final String NEWMAN_SERVER_REST_USER = "newman.agent.server-rest-user";
    private static final String DEFAULT_NEWMAN_SERVER_REST_USER = "root";
    private static final String NEWMAN_SERVER_REST_PW = "newman.agent.server-rest-pw";
    private static final String DEFAULT_NEWMAN_SERVER_REST_PW = "root";
    private static final String NEWMAN_AGENT_CAPABILITIES = "newman.agent.capabilities";
    private static final String NEWMAN_AGENT_DEFAULT_CAPABILITIES = "";

    private static final int NUM_OF_WORKERS = Integer.getInteger("newman.agent.workers", 5);
    private static final int JOB_POLL_INTERVAL = Integer.getInteger("newman.agent.job-poll-interval", 1000 * 10);
    private static final int RETRY_INTERVAL_ON_SUSPENDED = Integer.getInteger("newman.agent.retry-interval-on-suspended", 1000 * 60);
    private static final int PING_INTERVAL = Integer.getInteger("newman.agent.ping-interval", 1000 * 30);

    private Properties properties;

    public NewmanAgentConfig() {
        properties = new Properties();
        properties.putIfAbsent(NEWMAN_AGENT_HOST_NAME, loadHostName());
        properties.putIfAbsent(NEWMAN_AGENT_HOST_ADDRESS, loadHostAddress());
        properties.putIfAbsent(NEWMAN_HOME, getNonEmptySystemProperty(NEWMAN_HOME, DEFAULT_NEWMAN_HOME));
        properties.putIfAbsent(NEWMAN_SERVER_HOST, getNonEmptySystemProperty(NEWMAN_SERVER_HOST, DEFAULT_NEWMAN_SERVER_HOST));
        properties.putIfAbsent(NEWMAN_SERVER_PORT, getNonEmptySystemProperty(NEWMAN_SERVER_PORT, DEFAULT_NEWMAN_SERVER_PORT));
        properties.putIfAbsent(NEWMAN_SERVER_REST_USER, getNonEmptySystemProperty(NEWMAN_SERVER_REST_USER, DEFAULT_NEWMAN_SERVER_REST_USER));
        properties.putIfAbsent(NEWMAN_SERVER_REST_PW, getNonEmptySystemProperty(NEWMAN_SERVER_REST_PW, DEFAULT_NEWMAN_SERVER_REST_PW));
        properties.putIfAbsent(NEWMAN_AGENT_CAPABILITIES, getNonEmptySystemProperty(NEWMAN_AGENT_CAPABILITIES, NEWMAN_AGENT_DEFAULT_CAPABILITIES));
        properties.putIfAbsent(NEWMAN_AGENT_GROUPNAME, getNonEmptySystemProperty(NEWMAN_AGENT_GROUPNAME, "devGroup")); //ToDo return to empty string
    }

    private String loadHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknownHostName";
        }
    }

    static String getNetworkAddressFallback(String fallback) {
        if (System.getProperty("os.name").equals("Linux")) {
            try {
                StringBuilder sb = new StringBuilder();
                ProcessResult processResult = ProcessUtils.executeCommandAndWait("ip route get 8.8.8.8 | grep src| sed 's/.*src \\(.*\\)$/\\1/g'", 20 * 1000, sb);
                if (processResult.getExitCode() == null || processResult.getExitCode() != 0) {
                    return fallback;
                }
                return sb.toString().trim();
            } catch (Exception e) {
                return fallback;
            }
        }
        else {
            return fallback;
        }
    }

    public String loadHostAddress() {
        String res = "unknownHostAddress";
        System.out.println("trying to get hostname ");
        Enumeration<NetworkInterface> networkInterfaceEnumeration = null;
        try {
            networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            System.out.println("networkInterfaceEnumeration is: " + networkInterfaceEnumeration);
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        while (networkInterfaceEnumeration != null && networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface ni = null;
            try{
                ni = networkInterfaceEnumeration.nextElement();
                System.out.println("Searching at network Interface: " + ni.toString() );
                if(ni.isLoopback()) {
                    continue;
                }
                if(ni.isPointToPoint()) {
                    continue;
                }
                if(ni.getDisplayName() != null && ni.getDisplayName().contains("docker")) {
                    continue;
                }
                if(ni.getName().contains("docker")) {
                    continue;
                }
                if (ni.getName().startsWith("br-")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    System.out.println("INet Address: " + address);
                    if(address instanceof Inet4Address) {
                        String ip = address.getHostAddress();
                        System.out.println("found ip: " + ip);
                        return ip;
                    }
                }
            }
            catch (Exception e){
                System.out.println("Got exception when checked NetworkInterface: " + ni);
                e.printStackTrace();
            }
        }
        return getNetworkAddressFallback(res);
    }


    public int getPingInterval() {
        return PING_INTERVAL;
    }

    public String getHostName(){
        return properties.getProperty(NEWMAN_AGENT_HOST_NAME);
    }

    public String getHostAddress(){
        return properties.getProperty(NEWMAN_AGENT_HOST_ADDRESS);
    }

    public String getGroupName() {
        return properties.getProperty(NEWMAN_AGENT_GROUPNAME);
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

    public int getRetryIntervalOnSuspended() {
        return RETRY_INTERVAL_ON_SUSPENDED;
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

    public String getCapabilities() {
        return properties.getProperty(NEWMAN_AGENT_CAPABILITIES);
    }
}
