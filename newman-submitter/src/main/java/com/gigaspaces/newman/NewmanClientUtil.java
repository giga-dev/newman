package com.gigaspaces.newman;

import com.gigaspaces.newman.utils.EnvUtils;
import org.slf4j.Logger;

public class NewmanClientUtil {

    //connection env variables
    public static final String NEWMAN_HOST = "NEWMAN_HOST";
    public static final String NEWMAN_PORT = "NEWMAN_PORT";
    public static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    public static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";

    public static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public static NewmanClient getNewmanClient(Logger logger) throws Exception {
        // connection arguments
        String host = EnvUtils.getEnvironment(NEWMAN_HOST, true /*required*/, logger);
        String port = EnvUtils.getEnvironment(NEWMAN_PORT, true /*required*/, logger);
        String username = EnvUtils.getEnvironment(NEWMAN_USER_NAME, true /*required*/, logger);
        String password = EnvUtils.getEnvironment(NEWMAN_PASSWORD, true /*required*/, logger);

        return NewmanClient.create(host, port, username, password);
    }
}
