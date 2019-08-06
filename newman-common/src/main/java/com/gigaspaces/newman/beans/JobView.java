package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * @author evgenyf
 * 13.12.15.
 */

public class JobView {

    private String id;
    private String buildId;
    private String buildName;
    private String buildBranch;
    private String suiteId;
    private String suiteName;
    private String jobConfigId;
    private String jobConfigName;
    private Set<String> agentGroups;

    private Date submitTime;
    private Date startTime;
    private Date endTime;
    private URI testURI;
    private String submittedBy;
    private State state;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int failed3TimesTests;
    private int runningTests;
    private int numOfTestRetries;
    private Set<String> preparingAgents = Collections.emptySet();

    public JobView( Job job ) {

        id = job.getId();
        Build build = job.getBuild();
        Suite suite = job.getSuite();
        if( build != null ) {
            buildId = build.getId();
            buildName = build.getName();
            buildBranch = build.getBranch();
        }
        if( suite != null ) {
            suiteId = suite.getId();
            suiteName = suite.getName();
        }

        JobConfig jobConfig = job.getJobConfig();
        if(jobConfig !=null){
            jobConfigId=jobConfig.getId();
            jobConfigName = jobConfig.getName();
        }

        agentGroups = job.getAgentGroups();
        submitTime = job.getSubmitTime();
        startTime = job.getStartTime();
        endTime = job.getEndTime();
        testURI = job.getTestURI();
        submittedBy = job.getSubmittedBy();
        state = job.getState();
        totalTests = job.getTotalTests();
        passedTests = job.getPassedTests();
        failedTests = job.getFailedTests();
        failed3TimesTests = job.getFailed3TimesTests();
        runningTests = job.getRunningTests();
        numOfTestRetries = job.getNumOfTestRetries();
        preparingAgents = job.getPreparingAgents();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getAgentGroups() {
        return agentGroups;
    }

    public void setAgentGroups(Set<String> agentGroups) {
        this.agentGroups = agentGroups;
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

    public int getNumOfTestRetries() {return numOfTestRetries; }

    public void setNumOfTestRetries(int numOfTestRetries) { this.numOfTestRetries = numOfTestRetries; }

    public Set<String> getPreparingAgents() {
        return preparingAgents;
    }

    public void setPreparingAgents(Set<String> preparingAgents) {
        this.preparingAgents = preparingAgents;
    }

    public String getBuildId() {
        return buildId;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildBranch() {
        return buildBranch;
    }

    public String getSuiteId() {
        return suiteId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setBuildBranch(String buildBranch) {
        this.buildBranch = buildBranch;
    }

    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getJobConfigId() {
        return jobConfigId;
    }

    public void setJobConfigId(String jobConfigId) {
        this.jobConfigId = jobConfigId;
    }

    public String getJobConfigName() {
        return jobConfigName;
    }

    public void setJobConfigName(String jobConfigName) {
        this.jobConfigName = jobConfigName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("submitTime", submitTime)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .append("testURI", testURI)
                .append("submittedBy", submittedBy)
                .append("agentGRoups", agentGroups)
                .append("state", state)
                .append("totalTests", totalTests)
                .append("passedTests", passedTests)
                .append("failedTests", failedTests)
                .append("failed3TimesTests", failed3TimesTests)
                .append("runningTests", runningTests)
                .append("numOfTestRetries", numOfTestRetries)
                .append("preparingAgents", preparingAgents)
                .toString();
    }
}