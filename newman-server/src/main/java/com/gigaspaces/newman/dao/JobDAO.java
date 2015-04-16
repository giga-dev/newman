package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.Job;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class JobDAO extends BasicDAO<Job, ObjectId> {
    public JobDAO(Morphia morphia, MongoClient mongo, String db) {
        super(mongo, morphia, db);
    }
}
