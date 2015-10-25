package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;

/**
 * Created by tamirs
 * on 10/19/15.
 */

@Entity
public class FutureJob {
    @Id
    private String id;
    private String suiteID;
    private String buildID;
    private Date submitTime;
    private String author;

    public FutureJob() {
    }

    public FutureJob(String buildID, String suiteID, String author) {
        this.buildID = buildID;
        this.suiteID = suiteID;
        this.author = author;
        this.submitTime = new Date();
    }

    public String getSuiteID() {
        return suiteID;
    }

    public void setSuiteID(String suiteID) {
        this.suiteID = suiteID;
    }

    public String getBuildID() {
        return buildID;
    }

    public void setBuildID(String buildID) {
        this.buildID = buildID;
    }

    public String getId() {
        return id;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setId(String id) {
        this.id = id;
    }
}
