package com.gigaspaces.newman.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.*;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
@Table(name = "test", indexes = {
        @Index(name = "idx_job_id", columnList = "jobId"),
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_test_score", columnList = "testScore"),
        @Index(name = "idx_sha", columnList = "sha")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Test {
    public enum Status {PENDING, SUCCESS, FAIL, RUNNING}

    @Id
    private String id;

    private String jobId;
    private String name;
    private Double testScore;
    private String sha;

    @Type(type = "com.gigaspaces.newman.types.ListStringArrayType")
    @Column(name = "arguments", columnDefinition = "TEXT[]")
    private List<String> arguments;
    private String testType;
    private Long timeout;

    @Enumerated(value = EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private String historyStats;
    /* name, url mapping */

    @OneToOne(fetch = FetchType.EAGER,mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private TestLog logs;

    private String assignedAgent;
    private String agentGroup;
    private Date startTime;
    private Date endTime;
    private Date scheduledAt;
    private int progressPercent;
    private Integer runNumber = 1;

    @Type(type = "com.gigaspaces.newman.types.MapJsonType")
    @Column(name = "properties", columnDefinition = "JSON")
    private Map<String, String> properties;

    public Test() {
        properties = new HashMap<>();
        arguments = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        computeProgressPercent();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isLogsEmpty() {
        return logs == null;
    }

    public TestLog getLogs() {
        if (logs == null) logs = new TestLog(this);
        return logs;
    }

    public void setLogs(TestLog logs) {
        this.logs = logs;

        if (this.logs != null) {
            this.logs.setTest(this);
        }
    }

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(String assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public String getAgentGroup() { return agentGroup; }

    public void setAgentGroup(String agentGroup) { this.agentGroup = agentGroup; }

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

    public Date getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Date scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * set properties and replace keys to not contain dots due mongo impl - map keys can't contain dots
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    private void computeProgressPercent() {
        if (getStatus() != null) {
            switch (getStatus()) {
                case PENDING:
                    progressPercent = 0;
                    break;
                case SUCCESS:
                    progressPercent = 100;
                    break;
                case FAIL:
                    progressPercent = 100;
                    break;
                case RUNNING:
                    progressPercent = 50;
                    break;
            }
        }
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public Double getTestScore() {
        return testScore;
    }

    public void setTestScore(Double testScore) {
        this.testScore = testScore;
    }

    public String getHistoryStats() {
        return historyStats;
    }

    public void setHistoryStats(String historyStats) {
        this.historyStats = historyStats;
    }

    public Integer getRunNumber() {
        return runNumber;
    }

    public void setRunNumber(int runNumber) {
        this.runNumber = runNumber;
    }

    @PostLoad
    void postLoad() {
        computeProgressPercent();
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
                .append("jobId", jobId)
                .append("name", name)
                .append("arguments", arguments)
                .append("testType", testType)
                .append("timeout", timeout)
                .append("status", status)
                .append("errorMessage", errorMessage)
                .append("logs", logs)
                .append("assignedAgent", assignedAgent)
                .append("agentGroup", agentGroup)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .append("scheduledAt", scheduledAt)
                .append("sha", sha)
                .toString();
    }
}
