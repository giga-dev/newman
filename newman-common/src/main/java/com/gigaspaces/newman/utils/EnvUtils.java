package com.gigaspaces.newman.utils;

import static com.gigaspaces.newman.utils.StringUtils.isEmpty;
import org.slf4j.Logger;

/**
 * @author Boris
 * @since 1.0
 */
public class EnvUtils {

    public static String getEnvironment(String var, Logger logger) {
        return getEnvironment(var, true, logger);
    }

    public static String getEnvironment(String var, boolean required, Logger logger) {
        String v = System.getenv(var);
        if (isEmpty(v) && required) {
            logger.error("Please set the environment variable {} and try again.", var);
            throw new IllegalArgumentException("the environment variable " + var + " must be set");
        }
        return v;
    }
}
