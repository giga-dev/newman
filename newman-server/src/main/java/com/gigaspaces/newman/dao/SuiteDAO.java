package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Suite;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class SuiteDAO extends BasicDAO<Suite, ObjectId> {
    public SuiteDAO(Morphia morphia, MongoClient mongo, String db) {
        super(mongo, morphia, db);
    }
}
