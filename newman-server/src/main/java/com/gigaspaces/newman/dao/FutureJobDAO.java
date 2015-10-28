package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.FutureJob;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

/**
 * Created by tamirs
 * on 10/19/15.
 */
public class FutureJobDAO extends AbstractObjectIdDAO<FutureJob> {
    public FutureJobDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
