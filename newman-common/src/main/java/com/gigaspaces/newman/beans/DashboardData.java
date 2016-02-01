package com.gigaspaces.newman.beans;

import java.util.List;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 5/12/15.
 */
public class DashboardData {
    private List<Build> activeBuilds;
    private List<Build> pendingBuilds;
    private List<Build> historyBuilds;
    private List<FutureJob> futureJobs;
    //key is build id
    private Map<String,List<Job>> activeJobs;

    public DashboardData() {
    }

    public DashboardData(List<Build> activeBuilds, List<Build> pendingBuilds, List<Build> historyBuilds, Map<String,List<Job>> activeJobs, List<FutureJob> futureJobs) {
        this.activeBuilds = activeBuilds;
        this.pendingBuilds = pendingBuilds;
        this.historyBuilds = historyBuilds;
        this.activeJobs = activeJobs;
        this.futureJobs = futureJobs;
    }

    public List<Build> getActiveBuilds() {
        return activeBuilds;
    }

    public void setActiveBuilds(List<Build> activeBuilds) {
        this.activeBuilds = activeBuilds;
    }

    public List<Build> getPendingBuilds() {
        return pendingBuilds;
    }

    public void setPendingBuilds(List<Build> pendingBuilds) {
        this.pendingBuilds = pendingBuilds;
    }

    public List<Build> getHistoryBuilds() {
        return historyBuilds;
    }

    public void setHistoryBuilds(List<Build> historyBuilds) {
        this.historyBuilds = historyBuilds;
    }

    public Map<String, List<Job>> getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(Map<String, List<Job>> activeJobs) {
        this.activeJobs = activeJobs;
    }

    public List<FutureJob> getFutureJobs() {
        return futureJobs;
    }

    public void setFutureJobs(List<FutureJob> futureJobs) {
        this.futureJobs = futureJobs;
    }

    @Override
    public String toString() {
        return "DashboardData{" +
                "activeBuilds=" + activeBuilds +
                ", pendingBuilds=" + pendingBuilds +
                ", historyBuilds=" + historyBuilds +
                ", futureJobs=" + futureJobs +
                '}';
    }
}
