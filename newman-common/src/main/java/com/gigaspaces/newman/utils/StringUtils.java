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

}
