package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/7/15.
 */
@Entity
public class SuiteResult {
    private String id;
    private String runId;
    private List<TestResult> results;

}
