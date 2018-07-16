package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;

/**
 * Created by tamirs
 * on 10/19/15.
 */

@SuppressWarnings("ALL")
@Entity
public class FutureJob {
    @Id
    private String id;
    private String buildID;
    private String buildName;
    private String buildBranch;
    private String suiteID;
    private String suiteName;
    private String configID;
    private String configName;
    private String author;
    private Date submitTime;

    public FutureJob() {
    }

    public FutureJob(String buildID, String buildName, String buildBranch, String suiteID, String suiteName, String configID, String configName, String author) {
        this.buildID = buildID;
        this.buildName = buildName;
        this.buildBranch = buildBranch;
        this.suiteID = suiteID;
        this.suiteName = suiteName;
        this.author = author;
        this.submitTime = new Date();
        this.configID = configID;
        this.configName = configName;
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

    public String getBuildBranch() {
        return buildBranch;
    }

    public void setBuildBranch(String buildBranch) {
        this.buildBranch = buildBranch;
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

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getConfigID() {
        return configID;
    }

    public void setConfigID(String configID) {
        this.configID = configID;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }


    @Override
    public String toString() {
        return "FutureJob{" +
                "id='" + id + '\'' +
                ", buildID='" + buildID + '\'' +
                ", buildName='" + buildName + '\'' +
                ", buildBranch='" + buildBranch + '\'' +
                ", suiteID='" + suiteID + '\'' +
                ", suiteName='" + suiteName + '\'' +
                ", configID='" + configID + '\'' +
                ", configName='" + configName + '\'' +
                ", author='" + author + '\'' +
                ", submitTime=" + submitTime +
                '}';
    }

}
