package com.gigaspaces.newman.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.entities.BuildStatus;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuildStatusDTO {

    private String id;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int failed3TimesTests;
    private int runningTests;
    private int numOfTestRetries;

    private int totalJobs;
    private int pendingJobs;
    private int runningJobs;
    private int doneJobs;
    private int brokenJobs;

    private List<BuildStatusSuiteDTO> suites;

    public BuildStatusDTO(int passedTests, int failedTests, int failed3TimesTests) {
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.failed3TimesTests = failed3TimesTests;
    }

    // Constructor
    public BuildStatusDTO(String id, int totalTests, int passedTests, int failedTests,
                          int failed3TimesTests, int runningTests, int numOfTestRetries,
                          int totalJobs, int pendingJobs, int runningJobs, int doneJobs,
                          int brokenJobs, List<BuildStatusSuiteDTO> suites) {
        this.id = id;
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.failed3TimesTests = failed3TimesTests;
        this.runningTests = runningTests;
        this.numOfTestRetries = numOfTestRetries;
        this.totalJobs = totalJobs;
        this.pendingJobs = pendingJobs;
        this.runningJobs = runningJobs;
        this.doneJobs = doneJobs;
        this.brokenJobs = brokenJobs;
        this.suites = suites;
    }

    public static BuildStatusDTO fromEntity(BuildStatus buildStatus) {
        List<BuildStatusSuiteDTO> suitesDTO = buildStatus.getSuites().stream()
                .map(BuildStatusSuiteDTO::fromEntity) // Convert each BuildStatusSuite to BuildStatusSuiteDTO
                .collect(Collectors.toList());

        return new BuildStatusDTO(
                buildStatus.getId(),
                buildStatus.getTotalTests(),
                buildStatus.getPassedTests(),
                buildStatus.getFailedTests(),
                buildStatus.getFailed3TimesTests(),
                buildStatus.getRunningTests(),
                buildStatus.getNumOfTestRetries(),
                buildStatus.getTotalJobs(),
                buildStatus.getPendingJobs(),
                buildStatus.getRunningJobs(),
                buildStatus.getDoneJobs(),
                buildStatus.getBrokenJobs(),
                suitesDTO
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public void setFailed3TimesTests(int failed3TimesTests) {
        this.failed3TimesTests = failed3TimesTests;
    }

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

    public int getBrokenJobs() {
        return brokenJobs;
    }

    public void setBrokenJobs(int brokenJobs) {
        this.brokenJobs = brokenJobs;
    }

    public List<BuildStatusSuiteDTO> getSuites() {
        return suites;
    }

    public void setSuites(List<BuildStatusSuiteDTO> suites) {
        this.suites = suites;
    }
}
