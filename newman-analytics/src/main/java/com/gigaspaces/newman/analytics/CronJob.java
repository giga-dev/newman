package com.gigaspaces.newman.analytics;

import java.util.Properties;

/**
 * An interface of a cron job that will be instantiated by {@link NewmanAnalytics}.
 * The properties file must contain the cronjob-class key and name value to instantiate.
 *
 * Created by moran on 8/19/15.
 */
public interface CronJob {

    /**
     * This map will be auto-wired with the following:
     * properties-path : absolute file path to the properties file
     *
     * @param properties for this cronjob.
     */
    void run(Properties properties);
}
