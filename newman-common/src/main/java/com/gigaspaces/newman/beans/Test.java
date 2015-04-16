package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
public class Test {
    enum Status {PENDING, SUCCESS, FAIL}
    @Id
    private String id;
    private String jobId;
    private Status status;
    private String errorMessage;
    private List<URI> logs;
    private String agentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Test() {
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

    public List<URI> getLogs() {
        return logs;
    }

    public void setLogs(List<URI> logs) {
        this.logs = logs;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "Test{" +
                "jobId='" + jobId + '\'' +
                ", status=" + status +
                ", errorMessage='" + errorMessage + '\'' +
                ", logs=" + logs +
                ", agentId='" + agentId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
