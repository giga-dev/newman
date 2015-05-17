package com.gigaspaces.newman.dao;

import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;

/**
 * Created by Barak Bar Orion
 * 5/14/15.
 */
public abstract class AbstractObjectIdDAO<D> extends BasicDAO<D, ObjectId> {
    public AbstractObjectIdDAO(Morphia morphia, MongoClient mongo, String db) {
        super(mongo, morphia, db);
    }
    public Query<D> createIdQuery(String id){
        return createQuery().field("_id").equal(new ObjectId(id));
    }
}
