package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.Sha;
import com.gigaspaces.newman.utils.StringUtils;
import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.*;
import org.mongodb.morphia.utils.IndexDirection;

import java.util.*;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
public class Test {
    public enum Status {PENDING, SUCCESS, FAIL, RUNNING}

    @Id
    private String id;
    @Indexed(value = IndexDirection.ASC, unique = false)
    private String jobId;
    @Indexed(value = IndexDirection.ASC, unique = false)
    private String name;
    private List<String> arguments;
    private String testType;
    private Long timeout;
    private Status status;
    private String errorMessage;
    /* name, url mapping */
    @Embedded
    private Map<String, String> logs;
    private String assignedAgent;
    private Date startTime;
    private Date endTime;
    private Date scheduledAt;
    @Transient
    private int progressPercent;

    @Indexed(unique = false)
    private String sha;

    @Embedded
    private Map<String, String> properties;

    public Test() {
        logs = new HashMap<>();
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

    public Map<String, String> getLogs() {
        return logs;
    }

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

    private void computeSha() {
        if (sha == null && name != null && arguments != null) {
            sha = Sha.compute(name, arguments);
        }
    }

    @PostLoad
    void postLoad() {
        computeProgressPercent();
    }

    @PreSave
    void preSave() {
        computeSha();
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
