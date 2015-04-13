package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.time.LocalDateTime;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Run {
    public enum State {IN_QUEUE, DEPLOYING, RUNNING, UN_DEPLOYING, DONE, ABORTED}
    private String id;
    @Reference
    private Build build;
    @Reference
    private Platform platform;
    @Reference
    private Schedule schedule;
    @Reference
    private Suite suite;
    @Reference
    private SuiteResult suiteResult;
    private State state;
    private LocalDateTime startTime;
    private LocalDateTime startEndTime;

}
