package com.gigaspaces.newman.config;

/**
 * Created by Barak Bar Orion
 * 4/29/15.
 */
public class Mongo {
    private String host;
    private String db;

    public Mongo() {
        this.host = System.getProperty("newman.mongo.db.host", "localhost");
//        this.host = "pc-lab148";
        this.db = System.getProperty("newman.mongo.db.name", System.getProperty("user.name"));
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
