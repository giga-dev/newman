package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.PrioritizedJob;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;


public class PrioritizedJobDAO extends AbstractObjectIdDAO<PrioritizedJob> {
    public PrioritizedJobDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}