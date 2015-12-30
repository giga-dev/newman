package com.gigaspaces.newman.dao;

import com.gigaspaces.newman.beans.BuildsCache;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

/**
 * Created by Boris
 * on 28/12/15.
 */
public class BuildsCacheDAO extends AbstractObjectIdDAO<BuildsCache> {
    public BuildsCacheDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
