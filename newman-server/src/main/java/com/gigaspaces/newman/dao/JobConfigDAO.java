package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.JobConfig;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

public class JobConfigDAO extends AbstractObjectIdDAO<JobConfig>  {
    public JobConfigDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
