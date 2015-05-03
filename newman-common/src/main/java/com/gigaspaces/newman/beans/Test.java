package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.beans.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
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
    private String jobId;
    private String name;
    private Collection<String> arguments;
    private String testType;
    private long timeout;
    private Status status;
    private String errorMessage;
    /* name, url mapping */
    @Embedded
    private Map<String, String> logs;
    private String assignedAgent;
    private Date startTime;
    private Date endTime;
    private Date scheduledAt;

    public Test() {
        logs = new HashMap<>();
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

    public Collection<String> getArguments() {
        return arguments;
    }

    public void setArguments(Collection<String> arguments) {
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

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .append("jobId", jobId)
                .append("status", status)
                .append("errorMessage", errorMessage)
                .append("logs", logs)
                .append("assignedAgent", assignedAgent)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .append("scheduledAt", scheduledAt)
                .toString();
    }
}
