package com.gigaspaces.newman.usermanagment;

import com.gigaspaces.newman.dao.AbstractObjectIdDAO;
import com.mongodb.MongoClient;
import org.mongodb.morphia.Morphia;

public class UserDAO extends AbstractObjectIdDAO<User> {
    public UserDAO(Morphia morphia, MongoClient mongo, String db) {
        super(morphia, mongo, db);
    }
}
