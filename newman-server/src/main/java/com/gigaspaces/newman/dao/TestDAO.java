package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.Test;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class TestDAO extends AbstractObjectIdDAO<Test> {
    public TestDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
