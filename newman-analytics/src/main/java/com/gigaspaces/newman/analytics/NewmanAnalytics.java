package com.gigaspaces.newman.analytics;

import java.util.Properties;

/**
 * Created by moran on 8/19/15.
 */
public class NewmanAnalytics {

    private static final String CRONJOB_CLASS = "cronjob-class";

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            throw new IllegalArgumentException("Illegal usage; expected: <properties-file-path> <optionally followed by key=value args>");
        }

        Properties properties = new PropertiesConfigurer(args).getProperties();

        String cronableClass = properties.getProperty(CRONJOB_CLASS);
        if (cronableClass == null) {
            throw new IllegalArgumentException("Missing " + CRONJOB_CLASS +" key defined in " + args[0]);
        }

        CronJob cronJob = Class.forName(cronableClass).asSubclass(CronJob.class).newInstance();
        cronJob.run(properties);
    }
}
