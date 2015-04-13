package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

import java.net.URI;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Build {
    private String id;
    private Map<String, String> shas;
    private String branch;
    private URI uri;
}
