package com.gigaspaces.newman.config;

import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;

/**
 * Created by Barak Bar Orion
 * 4/29/15.
 */
public class Mongo {
    private String host;
    private String db;

    public Mongo() {
        this.host = getNonEmptySystemProperty("newman.mongo.db.host", "localhost");
        this.db = getNonEmptySystemProperty("newman.mongo.db.name", System.getProperty("user.name"));
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }
}
