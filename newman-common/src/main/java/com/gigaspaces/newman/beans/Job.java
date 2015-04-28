package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.*;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Job {
    @Id
    private String id;
    private Build build;
    private Collection<URI> resources;
    private Date submitTime;
    private Date startTime;
    private Date endTime;
    private URI testURI;
    private String submittedBy;
    private State state;

    public Job() {
        state = State.READY;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Build getBuild() {
        return build;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public URI getTestURI() {
        return testURI;
    }

    public void setTestURI(URI testURI) {
        this.testURI = testURI;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", build=" + build +
                ", submitTime=" + submitTime +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", testURI=" + testURI +
                ", submittedBy='" + submittedBy + '\'' +
                ", state=" + state +
                '}';
    }

    public Collection<URI> getResources() {
        return resources;
    }

    public void setResources(Collection<URI> resources) {
        this.resources = resources;
    }
}
