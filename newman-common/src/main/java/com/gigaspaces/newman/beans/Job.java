package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Job {
    @Id
    private String id;
    private Build build;
    private LocalDateTime submitTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private URI testURI;
    private String submittedBy;

    public Job() {
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

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
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
                '}';
    }
}
