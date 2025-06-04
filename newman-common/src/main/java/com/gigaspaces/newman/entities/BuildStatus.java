package com.gigaspaces.newman.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gigaspaces.newman.dto.BuildStatusDTO;
import com.gigaspaces.newman.dto.BuildStatusSuiteDTO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Barak Bar Orion
 * 5/14/15.
 */
@Entity
@Table(name = "build_status")
public class BuildStatus {

    @Id
    private String id;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int failed3TimesTests = 0;
    private int runningTests;
    private int numOfTestRetries = 0;

    private int totalJobs;
    private int pendingJobs;
    private int runningJobs;
    private int doneJobs;
    private int brokenJobs;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "build_id") // this will be a foreign key to Build.id
    private Build build;

    @Fetch(value = FetchMode.SELECT)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "build_status_id")
    private List<BuildStatusSuite> suites;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BuildStatus(Build build) {
        this();
        this.build = build;
    }

    public BuildStatus() {
        this.suites = new ArrayList<>();
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public BuildStatus incPassedTests() {
        this.passedTests++;
        return this;
    }

    public BuildStatus decPassedTests() {
        this.passedTests--;
        return this;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public BuildStatus incFailedTests() {
        this.failedTests++;
        return this;
    }

    public BuildStatus decFailedTests() {
        this.failedTests--;
        return this;
    }

    public int getFailed3TimesTests() {
        return failed3TimesTests;
    }

    public void setFailed3TimesTests(int failed3TimesTests) {
        this.failed3TimesTests = failed3TimesTests;
    }

    public BuildStatus incFailed3Tests() {
        this.failed3TimesTests++;
        return this;
    }

    public BuildStatus decFailed3Tests() {
        this.failed3TimesTests--;
        return this;
    }

    public int getRunningTests() {
        return runningTests;
    }

    public void setRunningTests(int runningTests) {
        this.runningTests = runningTests;
    }

    public BuildStatus incRunningTests() {
        this.runningTests++;
        return this;
    }

    public BuildStatus decRunningTests() {
        this.runningTests--;
        return this;
    }

    public int getNumOfTestRetries() {
        return numOfTestRetries;
    }

    public void setNumOfTestRetries(int numOfTestRetries) {
        this.numOfTestRetries = numOfTestRetries;
    }

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public BuildStatus incTotalJobs() {
        this.totalJobs++;
        return this;
    }

    public BuildStatus decTotalJobs() {
        this.totalJobs--;
        return this;
    }

    public int getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(int pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public BuildStatus incPendingJobs() {
        this.pendingJobs++;
        return this;
    }

    public BuildStatus decPendingJobs() {
        this.pendingJobs--;
        return this;
    }

    public int getRunningJobs() {
        return runningJobs;
    }

    public void setRunningJobs(int runningJobs) {
        this.runningJobs = runningJobs;
    }

    public BuildStatus incRunningJobs() {
        this.runningJobs++;
        return this;
    }

    public BuildStatus decRunningJobs() {
        this.runningJobs--;
        return this;
    }

    public int getDoneJobs() {
        return doneJobs;
    }

    public void setDoneJobs(int doneJobs) {
        this.doneJobs = doneJobs;
    }

    public BuildStatus decDoneJobs() {
        this.doneJobs--;
        return this;
    }

    public BuildStatus incDoneJobs() {
        this.doneJobs++;
        return this;
    }

    public List<BuildStatusSuite> getSuites() {
        return suites;
    }

    public void setSuites(List<BuildStatusSuite> suites) {
        this.suites = suites;
    }

    public int getBrokenJobs() {
        return brokenJobs;
    }

    public void setBrokenJobs(int brokenJobs) {
        this.brokenJobs = brokenJobs;
    }

    public Build getBuild() {
        return build;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public BuildStatus decBrokenJobs() {
        this.brokenJobs--;
        return this;
    }

    public BuildStatus incBrokenJobs() {
        this.brokenJobs++;
        return this;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("totalTests", totalTests)
                .append("passedTests", passedTests)
                .append("failedTests", failedTests)
                .append("failed3TimesTests", failed3TimesTests)
                .append("runningTests", runningTests)
                .append("numOfTestRetries", numOfTestRetries)
                .append("totalJobs", totalJobs)
                .append("pendingJobs", pendingJobs)
                .append("runningJobs", runningJobs)
                .append("doneJobs", doneJobs)
                .append("brokenJobs", brokenJobs)
                .append("suites", suites)
                .toString();
    }
}
