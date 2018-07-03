package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

import java.util.Date;
import java.util.List;

/**
 * @author evgenyf
 * 13.12.2015
 */
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
    private Date startTime;
    private Date endTime;
    private int progressPercent;
    private int runNumber;

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
        startTime = test.getStartTime();
        endTime = test.getEndTime();
        progressPercent = test.getProgressPercent();
        runNumber = test.getRunNumber();
        computeProgressPercent(test.getStatus());
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

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName(), true)
                .append("id", id)
                .append("jobId", jobId)
                .append("name", name)
                .append("arguments", arguments)
                .append("status", status)
                .append("errorMessage", errorMessage)
                .append("assignedAgent", assignedAgent)
                .append("startTime", startTime)
                .append("endTime", endTime)
                .toString();
    }
}
