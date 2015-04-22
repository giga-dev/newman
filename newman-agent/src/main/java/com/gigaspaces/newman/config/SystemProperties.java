package com.gigaspaces.newman.config;

/**
 * Created by boris on 4/20/2015.
 */
public class SystemProperties {

    public final static String WORKERS_POOL_SIZE = "com.gs.newman.agent.workersPoolSize";

    public final static int DEFAULT_WORKERS_POOL_SIZE = 5;

    /**
     * In mills
      */
    public final static String FETCH_JOB_INTERVAL = "com.gs.newman.agent.fetchJobInterval";

    public final static int DEFAULT_FETCH_JOB_INTERVAL = 2000;

    public final static String WORKERS_POLLING_INTERVAL = "com.gs.newman.agent.workersPollingInterval";

    public final static int DEFAULT_WORKERS_POLLING_INTERVAL = 1000;

    public static final String NEWMAN_DIRECTORY = "com.gs.newman.agent.buildDirectory";
}
