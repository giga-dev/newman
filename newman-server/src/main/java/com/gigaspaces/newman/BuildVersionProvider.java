package com.gigaspaces.newman;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildVersionProvider {

    private static final String VERSION;

    static {
        String version = "unknown";
        try (InputStream is = BuildVersionProvider.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                version = props.getProperty("version", version);
            }
        } catch (IOException e) {
            // log if needed
        }
        VERSION = version;
    }

    public static String getVersion() {
        return VERSION;
    }
}
