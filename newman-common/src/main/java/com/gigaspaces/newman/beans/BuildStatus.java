package com.gigaspaces.newman.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Barak Bar Orion
 * 5/14/15.
 */
public class BuildStatus {

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

    private List<String> suitesNames;
    private List<String> suitesIds;

    public BuildStatus(){
        this.suitesNames = new ArrayList<>();
        this.suitesIds = new ArrayList<>();
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

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public int getFailed3TimesTests() {
        return failed3TimesTests;
    }

    public void setFailed3TimesTests(int failed3TimesTests) { this.failed3TimesTests = failed3TimesTests; }

    public int getRunningTests() {
        return runningTests;
    }

    public void setRunningTests(int runningTests) {
        this.runningTests = runningTests;
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

    public int getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(int pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public int getRunningJobs() {
        return runningJobs;
    }

    public void setRunningJobs(int runningJobs) {
        this.runningJobs = runningJobs;
    }

    public int getDoneJobs() {
        return doneJobs;
    }

    public void setDoneJobs(int doneJobs) {
        this.doneJobs = doneJobs;
    }

    public List<String> getSuitesNames() {
        return suitesNames;
    }

    public void setSuitesNames(List<String> suitesNames) {
        this.suitesNames = suitesNames;
    }

    public List<String> getSuitesIds() {
        return suitesIds;
    }

    public void setSuitesIds(List<String> suitesIds) {
        this.suitesIds = suitesIds;
    }

    public int getBrokenJobs() {
        return brokenJobs;
    }

    public void setBrokenJobs(int brokenJobs) {
        this.brokenJobs = brokenJobs;
    }

    @Override
    public String toString() {
        return "BuildStatus{" +
                "totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", failed3TimesTests=" + failed3TimesTests +
                ", runningTests=" + runningTests +
                ", numOfTestRetries=" + numOfTestRetries +
                ", totalJobs=" + totalJobs +
                ", pendingJobs=" + pendingJobs +
                ", runningJobs=" + runningJobs +
                ", doneJobs=" + doneJobs +
                ", brokenJobs=" + brokenJobs +
                ", suitesNames=" + suitesNames +
                ", suitesIds=" + suitesIds +
                '}';
    }
}