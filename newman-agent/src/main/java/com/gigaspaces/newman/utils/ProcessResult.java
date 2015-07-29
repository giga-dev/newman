package com.gigaspaces.newman.utils;

public class ProcessResult {
    private Integer exitCode;
    private long startTime;
    private long endTime;

    public Integer getExitCode() {
        return exitCode;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "ProcessResult{" +
                "exitCode=" + exitCode +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
