package com.gigaspaces.newman.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.projections.PTest;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestDTO {

    private String id;
    private String jobId;
    private String name;
    private List<String> arguments;
    private String status;
    private String errorMessage;
    private double testScore;
    private String historyStats;
    private String assignedAgent;
    private String agentGroup;
    private Date startTime;
    private Date endTime;
    private int progressPercent;
    private int runNumber;

    public TestDTO(PTest projection) {
        this.id = projection.getId();
        this.jobId = projection.getJobId();
        this.name = projection.getName();
        this.arguments = projection.getArguments();
        this.status = projection.getStatus();
        this.errorMessage = projection.getErrorMessage();
        this.testScore = projection.getTestScore();
        this.historyStats = projection.getHistoryStats();
        this.assignedAgent = projection.getAssignedAgent();
        this.agentGroup = projection.getAgentGroup();
        this.startTime = projection.getStartTime();
        this.endTime = projection.getEndTime();
        this.progressPercent = projection.getProgressPercent();
        this.runNumber = projection.getRunNumber();
    }

    // Getters and optionally setters (if needed)
    public String getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public String getName() {
        return name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getTestScore() {
        return testScore;
    }

    public String getHistoryStats() {
        return historyStats;
    }

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public String getAgentGroup() {
        return agentGroup;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public int getRunNumber() {
        return runNumber;
    }
}
