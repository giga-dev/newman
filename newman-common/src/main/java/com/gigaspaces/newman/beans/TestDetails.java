package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.StringUtils;
import com.gigaspaces.newman.utils.ToStringBuilder;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
*  @author evgenyf
 */
public class TestDetails {

    private String id;
    private String jobId;
    private String name;
    private List<String> arguments;
    private String testType;
    private Long timeout;
    private Test.Status status;
    private String errorMessage;
    private Double testScore;
    private String historyStats;
    private Map<String, String> logs;
    private String assignedAgent;
    private Date startTime;
    private Date endTime;
    private Date scheduledAt;
    private int progressPercent;
    private String sha;
    private Map<String, String> properties;

    public TestDetails( Test test ) {
        this.id = test.getId();
        this.jobId = test.getJobId();
        this.name = test.getName();
        this.arguments = test.getArguments();
        this.testType = test.getTestType();
        this.timeout = test.getTimeout();
        this.status = test.getStatus();
        this.errorMessage = test.getErrorMessage();
        this.testScore = test.getTestScore();
        this.historyStats = test.getHistoryStats();
        this.logs = test.getLogs();
        this.assignedAgent = test.getAssignedAgent();
        this.startTime = test.getStartTime();
        this.endTime = test.getEndTime();
        this.scheduledAt = test.getScheduledAt();
        computeProgressPercent(test.getStatus());
        this.sha = test.getSha();
        this.properties = test.getProperties();
    }

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public List<String> getArguments() {
        return arguments;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Test.Status getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, String> getLogs() {
        return logs;
    }

    @SuppressWarnings("unused")
    public void setLogs(Map<String, String> logs) {
        this.logs = logs;
    }

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(String assignedAgent) {
        this.assignedAgent = assignedAgent;
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
        if (properties == null) return;

        String[] keyArray = properties.keySet().toArray(new String[properties.size()]);
        for (String orginalKeys : keyArray) {
            String newKey = StringUtils.dotToDash(orginalKeys);
            if (!newKey.equals(orginalKeys)) {
                properties.put(newKey, properties.get(orginalKeys));
                properties.remove(orginalKeys);
            }
        }
        this.properties = properties;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    private void computeProgressPercent( Test.Status status ) {
        if (status != null) {
            switch (status) {
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


    public Double getTestScore() {
        return testScore;
    }

    public String getHistoryStats() {
        return historyStats;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName(), true)
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
                .append("startTime", startTime)
                .append("endTime", endTime)
                .append("scheduledAt", scheduledAt)
                .append("sha", sha)
                .toString();
    }
}
