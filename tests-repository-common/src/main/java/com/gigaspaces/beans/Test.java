package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

import java.net.URI;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Test {
    private String id;
    private URI source;
    private TestRunner runner;
    private TestParams params;

}
