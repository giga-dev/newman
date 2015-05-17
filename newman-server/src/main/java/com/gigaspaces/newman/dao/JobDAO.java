package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.Job;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class JobDAO extends AbstractObjectIdDAO<Job> {
    public JobDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
