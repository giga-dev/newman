package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

import java.net.URI;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class TestResult {
    public enum Status {PASSED, FAILED, ERROR}
    private String id;
    private String message;
    private String exception;
    private Map<String, URI> logs;
    private Status status;
}
