package com.gigaspaces.newman.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
    @Id
    private String id;
    @Embedded(concreteClass = Build.class)
    private Build build;

    @Embedded(concreteClass = Suite.class) //Why isn't this @Reference?, it should be reference!
    private Suite suite;

    private Set<String> agentGroups;
    private int priority;

    @Indexed
    private Date submitTime;
    private Date startTime;
    private Date endTime;
    private URI testURI;
    private String submittedBy;
    private State state;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int failed3TimesTests = 0;
    private int runningTests;
    private int numOfTestRetries = 0;
    // first agent start prepare on job
    private Date startPrepareTime;
    // last time job seen as zombie
    private Date lastTimeZombie;
    @Embedded
    private Set<String> preparingAgents = Collections.emptySet();
    @Transient
    private Set<String> agents = Collections.emptySet();
    @Embedded
    private Map<String, String> jobSetupLogs;

    @Embedded(concreteClass = JobConfig.class)
    private JobConfig jobConfig;

    public Job() {
        state = State.READY;
    }

    public void setAgentGroups(Set<String> agentGroups) { this.agentGroups = agentGroups; }

    public Set<String> getAgentGroups() { return agentGroups; }

    public int getPriority() { return priority; }

    public void setPriority(int priority) { this.priority = priority; }

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

    public int getFailed3TimesTests() { return failed3TimesTests;}

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

    public Suite getSuite() {
        return suite;
    }

    public void setSuite(Suite suite) {
        this.suite = suite;
    }

    public Set<String> getPreparingAgents() {
        return preparingAgents;
    }

    public void setPreparingAgents(Set<String> preparingAgents) {
        this.preparingAgents = preparingAgents;
    }

    public Date getStartPrepareTime() {
        return startPrepareTime;
    }

    public void setStartPrepareTime(Date startPrepareTime) {
        this.startPrepareTime = startPrepareTime;
    }

    public Date getLastTimeZombie() {
        return lastTimeZombie;
    }

    public void setLastTimeZombie(Date lastTimeZombie) {
        this.lastTimeZombie = lastTimeZombie;
    }

    public Map<String, String> getJobSetupLogs() {
        return jobSetupLogs;
    }

    public void setJobSetupLogs(Map<String, String> jobSetupLogs) {
        this.jobSetupLogs = jobSetupLogs;
    }

    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public void setJobConfig(JobConfig jobConfig) {
        this.jobConfig = jobConfig;
    }

    public Set<String> getAgents() { return agents; }

    public void setAgents(Set<String> agents) { this.agents = agents; }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", build=" + build +
                ", suite=" + suite +
                ", submitTime=" + submitTime +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", testURI=" + testURI +
                ", submittedBy='" + submittedBy + '\'' +
                ", state=" + state +
                ", totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", failed3TimesTests=" + failed3TimesTests +
                ", runningTests=" + runningTests +
                ", numOfTestRetries=" + numOfTestRetries +
                ", startPrepareTime=" + startPrepareTime +
                ", lastTimeZombie=" + lastTimeZombie +
                ", preparingAgents=" + preparingAgents +
                ", agents=" + agents +
                ", agentGroups=" + agentGroups +
                ", priority=" + priority +
                ", jobSetupLogs=" + jobSetupLogs +
                ", jobConfig=" + jobConfig +
                '}';
    }
}
