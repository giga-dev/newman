package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.Test;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class TestDAO extends BasicDAO<Test, ObjectId> {
    public TestDAO(Morphia morphia, MongoClient mongo, String db) {
        super(mongo, morphia, db);
    }
}
