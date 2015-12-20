package com.gigaspaces.newman.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * A properties file configuration factory.
 * It expects args[0] to be the properties file location
 * Any arguments following should be key=value pairs (e.g. myprop=myval)
 * The key-value pairs override any existing property in the properties file; or added if non-existent.
 *
 * Created by moran on 8/19/15.
 */
public class PropertiesConfigurer {

    public static final String PROPERTIES_PATH = "properties-path";

    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfigurer.class);
    private final Properties properties = new Properties();

    public PropertiesConfigurer(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing reporter properties file argument");
        }

        FileInputStream fileInputStream = null;
        try {
            String propertiesFile = args[0];
            fileInputStream = new FileInputStream(propertiesFile);
            properties.load(fileInputStream);
            properties.put(PROPERTIES_PATH, new File(propertiesFile).getParentFile().getAbsolutePath());

            if (logger.isDebugEnabled()) {
                logger.debug("loaded properties from {}\nproperties: {}", propertiesFile, prettyString(properties));
            } else {
                logger.info("loaded properties from {}", propertiesFile);
            }
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }

        overridePropertiesWithArgs(properties, args);
        overridePropertiesWithSysProps(properties);
        logger.info("using properties: {}", prettyString(properties));
    }

    public Properties getProperties() {
        return properties;
    }

    private static String prettyString(Properties properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Map.Entry<Object, Object> entry: properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.contains("password")) {
                value = "*****";
            }
            sb.append("\t").append(key).append(" = ").append(value).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static void overridePropertiesWithArgs(Properties properties, String[] args) {
        for (int i=1; i<args.length; i++) {
            String[] keyValuePair = args[i].split("=");
            if (keyValuePair.length != 2) {
                throw new IllegalArgumentException("expected argument as key=value pair for " + args[i]);
            }
            String key = keyValuePair[0];
            String value = keyValuePair[1];
            boolean exists = properties.containsKey(key);

            properties.setProperty(key, value);
            if (exists) {
                logger.debug("override property: {} = {}", key, value);
            } else {
                logger.debug("adding property: {} = {}", key, value);
            }
        }
    }

    private static void overridePropertiesWithSysProps(Properties properties) {
        for (Map.Entry<Object, Object> entry: properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (value.startsWith("${") && value.endsWith("}")) {
                value = value.substring(2, value.length() -1);
            } else {
                continue;
            }
            String newValue = System.getProperty(value);
            if (newValue != null) {
                logger.debug("replacing property: {} = {}", key, newValue);
                properties.setProperty(key, newValue);
            } else {
                logger.warn("no replacement for property: ${{}}", key);
            }
        }
    }
}
