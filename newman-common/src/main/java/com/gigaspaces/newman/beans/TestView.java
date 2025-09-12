package com.gigaspaces.newman.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.entities.Test;
import com.gigaspaces.newman.projections.PTest;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;
import java.util.List;

/**
 * @author evgenyf
 * 13.12.2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestView {

    private String id;
    private String jobId;
    private String name;
    private List<String> arguments;
    private Test.Status status;
    private String errorMessage;
    private Double testScore;
    private String historyStats;
    private String assignedAgent;
    private String agentGroup;
    private Date startTime;
    private Date endTime;
    private int progressPercent;
    private int runNumber;
    private boolean hasLogs;

    public TestView( Test test ) {

        id = test.getId();
        jobId = test.getJobId();
        name = test.getName();
        arguments = test.getArguments();
        status = test.getStatus();
        errorMessage = test.getErrorMessage();
        testScore = test.getTestScore();
        historyStats = test.getHistoryStats();
        assignedAgent = test.getAssignedAgent();
        agentGroup = test.getAgentGroup();
        startTime = test.getStartTime();
        endTime = test.getEndTime();
        progressPercent = test.getProgressPercent();
        runNumber = test.getRunNumber();
        computeProgressPercent(test.getStatus());
        hasLogs = test.getLogs().getTestLogs().size() > 0;
    }

    public TestView( PTest test ) {

        id = test.getId();
        jobId = test.getJobId();
        name = test.getName();
        arguments = test.getArguments();
        status = Test.Status.valueOf(test.getStatus());
        errorMessage = test.getErrorMessage();
        testScore = test.getTestScore();
        historyStats = test.getHistoryStats();
        assignedAgent = test.getAssignedAgent();
        agentGroup = test.getAgentGroup();
        startTime = test.getStartTime();
        endTime = test.getEndTime();
        progressPercent = test.getProgressPercent();
        runNumber = test.getRunNumber();
        computeProgressPercent(status);
        hasLogs = test.getLogs().getTestLogs().size() > 0;
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

    public String getAgentGroup() { return agentGroup; }

    public void setAgentGroup(String agentGroup) { this.agentGroup = agentGroup; }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Test.Status getStatus() {
        return status;
    }

    public void setStatus(Test.Status status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    private void computeProgressPercent( Test.Status status ) {
        if ( status != null) {
            switch ( status ) {
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

    public int getRunNumber() {
        return runNumber;
    }

    public void setRunNumber(int runNumber) {
        this.runNumber = runNumber;
    }

    public boolean isHasLogs() {
        return hasLogs;
    }

    public void setHasLogs(boolean hasLogs) {
        this.hasLogs = hasLogs;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("jobId", jobId)
                .append("name", name)
                .append("arguments", arguments)
                .append("status", status)
                .append("errorMessage", errorMessage)
                .append("assignedAgent", assignedAgent)
                .append("agentGroup", agentGroup)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .append("hasLogs", hasLogs)
                .toString();
    }
}
