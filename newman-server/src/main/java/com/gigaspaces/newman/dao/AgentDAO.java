package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.Agent;
import com.gigaspaces.newman.beans.Build;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */
public class AgentDAO extends AbstractObjectIdDAO<Agent> {
    public AgentDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
