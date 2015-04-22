package com.gigaspaces.newman.config;

import com.gigaspaces.newman.NewmanAgent;
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
public class AgentConfig {

    private Properties properties;
    private static final Logger logger = LoggerFactory.getLogger(NewmanAgent.class);
    public static final String fileSeperator = File.separator;
    private static final String DEFAULT_NEWMAN_DIR = System.getProperty("user.home") + fileSeperator
            + "tmp" + fileSeperator + "newman-agent";

    public AgentConfig(String propsFilePath) {
        properties = new Properties();
        loadPropertiesFile(propsFilePath);
        setHostNameProperty();
        setNewmanLocationProperty();
    }

    private void setHostNameProperty() {
        try
        {
            properties.put("hostName", InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException e)
        {
            properties.put("hostName", "unknown");
        }
    }

    private void loadPropertiesFile(String propsFilePath) {
        if ( propsFilePath == null || !new File( propsFilePath ).isFile() )
        {
            logger.warn("Properties URL is not found [" + propsFilePath + "]. Use default configuration.");

        }
        else
        {
            try
            {
                FileInputStream url = new FileInputStream( new File( propsFilePath ) );
                properties.load(url);
            }
            catch( IOException ex )
            {
                throw new IllegalStateException("Failed to load configuration file.", ex);
            }
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public String getHostName(){
        return properties.getProperty("hostName");
    }

    public void setNewmanLocationProperty() {
        if (properties.getProperty("newmanLocation") == null){
            properties.put("newmanLocation", System.getProperty(SystemProperties.NEWMAN_DIRECTORY, DEFAULT_NEWMAN_DIR));
        }
    }

    public String getNewmanLocation() {
        return properties.getProperty("newmanLocation");
    }

    public String getBuildLocation() {
        return getNewmanLocation() + fileSeperator + "build";
    }

    public String getTestsLocation() {
        return getNewmanLocation() + fileSeperator + "tests";
    }
}
