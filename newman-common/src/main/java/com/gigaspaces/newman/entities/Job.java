package com.gigaspaces.newman.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gigaspaces.newman.beans.State;
import com.gigaspaces.newman.converters.UriToStringConverter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;
import java.util.*;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
@Table(name = "job", indexes = {
        @Index(name = "idx_submit_time", columnList = "submit_time")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "build_id")
    private Build build;

    @ManyToOne
    @JoinColumn(name = "suite_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(
                    name = "fk_job_suite",
                    foreignKeyDefinition = "FOREIGN KEY (suite_id) REFERENCES suite(id) ON DELETE SET NULL"
                ))
    @NotFound(action = NotFoundAction.IGNORE)
    private Suite suite;

    @Column(name = "suite_id")
    private String suiteId;

    @Column(name = "suite_name")
    private String suiteName;

    @Type(type = "com.gigaspaces.newman.types.SetStringArrayType")
    @Column(name = "agent_groups", columnDefinition = "TEXT[]")
    private Set<String> agentGroups;
    private int priority;

    @Column(name = "submit_time")
    private Date submitTime;
    private Date startTime;
    private Date endTime;
    @Convert(converter = UriToStringConverter.class)
    private URI testURI;
    private String submittedBy;
    @Enumerated(EnumType.STRING)
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

    @Type(type = "com.gigaspaces.newman.types.SetStringArrayType")
    @Column(name = "preparing_agents", columnDefinition = "TEXT[]")
    private Set<String> preparingAgents = Collections.emptySet();;

    @Transient
    private Set<String> agents = Collections.emptySet();

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private JobSetupLog jobSetupLog;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_config_id")
    private JobConfig jobConfig;

    public Job(String id, String suiteId, String suiteName, String buildId, String buildName, String buildBranch) {
        this.id = id;
        this.suiteId = suiteId;
        this.suiteName = suiteName;

        this.build = new Build();
        this.build.setId(buildId);
        this.build.setName(buildName);
        this.build.setBranch(buildBranch);
    }

    public Job() {
        this.state = State.READY;
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

    public Job setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
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

    public Job setState(State state) {
        this.state = state;
        return this;
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

    public Job incPassedTests() {
        this.passedTests++;
        return this;
    }

    public Job decPassedTests() {
        this.passedTests--;
        return this;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public Job incFailedTests() {
        this.failedTests++;
        return this;
    }

    public Job decFailedTests() {
        this.failedTests--;
        return this;
    }

    public int getFailed3TimesTests() { return failed3TimesTests;}

    public void setFailed3TimesTests(int failed3TimesTests) { this.failed3TimesTests = failed3TimesTests; }

    public Job incFailed3Tests() {
        this.failed3TimesTests++;
        return this;
    }

    public Job decFailed3Tests() {
        this.failed3TimesTests--;
        return this;
    }

    public int getRunningTests() {
        return runningTests;
    }

    public void setRunningTests(int runningTests) {
        this.runningTests = runningTests;
    }

    public Job incRunningTests() {
        this.runningTests++;
        return this;
    }

    public Job decRunningTests() {
        this.runningTests--;
        return this;
    }

    public int getNumOfTestRetries() {
        return numOfTestRetries;
    }

    public void setNumOfTestRetries(int numOfTestRetries) {
        this.numOfTestRetries = numOfTestRetries;
    }

    public String getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public Suite getSuite() {
        if (suite != null) {
            return suite;
        }
        if (suiteId == null && suiteName == null) {
            return null;
        }
        return new Suite(this.suiteId, this.suiteName);
    }

    public void setSuite(Suite suite) {
        this.suite = suite;
        if (suite != null) {
            this.suiteId = suite.getId();
            this.suiteName = suite.getName();
        }
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

    public JobSetupLog getJobSetupLog() {
        return jobSetupLog;
    }

    public void setJobSetupLog(JobSetupLog jobSetupLog) {
        this.jobSetupLog = jobSetupLog;
    }

    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public void setJobConfig(JobConfig jobConfig) {
        this.jobConfig = jobConfig;
    }

    public Set<String> getAgents() { return agents; }

    public void setAgents(Set<String> agents) { this.agents = agents; }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }

        if (jobSetupLog == null) {
            jobSetupLog = new JobSetupLog(this);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("build", build)
                .append("suiteId", suiteId)
                .append("suiteName", suiteName)
                .append("agentGroups", agentGroups)
                .append("priority", priority)
                .append("submitTime", submitTime)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .append("testURI", testURI)
                .append("submittedBy", submittedBy)
                .append("state", state)
                .append("totalTests", totalTests)
                .append("passedTests", passedTests)
                .append("failedTests", failedTests)
                .append("failed3TimesTests", failed3TimesTests)
                .append("runningTests", runningTests)
                .append("numOfTestRetries", numOfTestRetries)
                .append("startPrepareTime", startPrepareTime)
                .append("lastTimeZombie", lastTimeZombie)
                .append("preparingAgents", preparingAgents)
                .append("agents", agents)
                .append("jobSetupLog", jobSetupLog)
                .append("jobConfig", jobConfig)
                .toString();
    }
}
