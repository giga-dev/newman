package com.gigaspaces.newman;

import java.util.Date;

public class FailedPreparingAgent {

    private String agentName;
    private String jobId;
    private String suiteName;
    private String buildId;
    private String buildName;
    private String buildBranch;
    private int prepareFailCountAtFailure;
    private Date lastTouchTime;
    private Long activeDurationMs;
    private Date failedAt;
    private String reason;

    public FailedPreparingAgent() {
    }

    public FailedPreparingAgent(String agentName, String jobId, String suiteName, String buildId, String buildName, String buildBranch,
                                 int prepareFailCountAtFailure, Date lastTouchTime, Long activeDurationMs, Date failedAt, String reason) {
        this.agentName = agentName;
        this.jobId = jobId;
        this.suiteName = suiteName;
        this.buildId = buildId;
        this.buildName = buildName;
        this.buildBranch = buildBranch;
        this.prepareFailCountAtFailure = prepareFailCountAtFailure;
        this.lastTouchTime = lastTouchTime;
        this.activeDurationMs = activeDurationMs;
        this.failedAt = failedAt;
        this.reason = reason;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildBranch() {
        return buildBranch;
    }

    public void setBuildBranch(String buildBranch) {
        this.buildBranch = buildBranch;
    }

    public int getPrepareFailCountAtFailure() {
        return prepareFailCountAtFailure;
    }

    public void setPrepareFailCountAtFailure(int prepareFailCountAtFailure) {
        this.prepareFailCountAtFailure = prepareFailCountAtFailure;
    }

    public Date getLastTouchTime() {
        return lastTouchTime;
    }

    public void setLastTouchTime(Date lastTouchTime) {
        this.lastTouchTime = lastTouchTime;
    }

    public Long getActiveDurationMs() {
        return activeDurationMs;
    }

    public void setActiveDurationMs(Long activeDurationMs) {
        this.activeDurationMs = activeDurationMs;
    }

    public Date getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Date failedAt) {
        this.failedAt = failedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
