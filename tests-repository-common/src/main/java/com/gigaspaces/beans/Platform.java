package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Platform {
    private String id;
    enum JDK {ORACLE_JDK8, ORACLE_JDK6};
    private JDK jdk;

}
