package com.gigaspaces.newman.beans;

import java.util.List;

/**
 * Created by Barak Bar Orion
 * 5/12/15.
 */
public class DashboardData {
    private List<BuildWithJobs> activeBuilds;
    private List<BuildWithJobs> historyBuilds;

    public DashboardData() {
    }

    public List<BuildWithJobs> getActiveBuilds() {
        return activeBuilds;
    }

    public void setActiveBuilds(List<BuildWithJobs> activeBuilds) {
        this.activeBuilds = activeBuilds;
    }

    public List<BuildWithJobs> getHistoryBuilds() {
        return historyBuilds;
    }

    public void setHistoryBuilds(List<BuildWithJobs> historyBuilds) {
        this.historyBuilds = historyBuilds;
    }

    @Override
    public String toString() {
        return "DashboardData{" +
                "activeBuilds=" + activeBuilds +
                ", historyBuilds=" + historyBuilds +
                '}';
    }
}
