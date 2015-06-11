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
    private int runningTests;

    private int totalJobs;
    private int pendingJobs;
    private int runningJobs;
    private int doneJobs;

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

    public int getRunningTests() {
        return runningTests;
    }

    public void setRunningTests(int runningTests) {
        this.runningTests = runningTests;
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

    @Override
    public String toString() {
        return "BuildStatus{" +
                "totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", runningTests=" + runningTests +
                ", totalJobs=" + totalJobs +
                ", pendingJobs=" + pendingJobs +
                ", runningJobs=" + runningJobs +
                ", doneJobs=" + doneJobs +
                ", suitesNames=" + suitesNames +
                ", suitesIds=" + suitesIds +
                '}';
    }
}