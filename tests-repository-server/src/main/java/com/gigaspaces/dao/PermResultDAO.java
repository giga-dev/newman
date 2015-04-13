package com.gigaspaces.dao;

import com.gigaspaces.beans.PermResult;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class PermResultDAO extends BasicDAO<PermResult, ObjectId> {
    public PermResultDAO(Morphia morphia, MongoClient mongo, String db) {
        super(mongo, morphia, db);
    }
}
