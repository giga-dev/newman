package com.gigaspaces.newman.utils;

public class StringUtils {

    public static String dotToDash(String exp){
        return exp.replace(".", "-");
    }

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }
    public static boolean notEmpty(String string) {
        return string != null && string.length() != 0;
    }

    public static String getNonEmptySystemProperty(String key, String def) {
        String property = System.getProperty(key);
        if (notEmpty(property)) {
            return property;
        } else {
            return def;
        }
    }

    public static String getRequiredSystemProperty(String key) {
        String val = System.getProperty(key);
        if (val == null) {
            throw new IllegalArgumentException("the system property " + key + " must be set");
        }
        return val;
    }
}
