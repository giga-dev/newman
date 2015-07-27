package com.gigaspaces.newman.beans;

import java.util.List;

/**
 * Created by Barak Bar Orion
 * 5/12/15.
 */
public class DashboardData {
    private List<Build> activeBuilds;
    private List<Build> pendingBuilds;
    private List<Build> historyBuilds;

    public DashboardData() {
    }

    public DashboardData(List<Build> activeBuilds, List<Build> pendingBuilds, List<Build> historyBuilds) {
        this.activeBuilds = activeBuilds;
        this.pendingBuilds = pendingBuilds;
        this.historyBuilds = historyBuilds;
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

    @Override
    public String toString() {
        return "DashboardData{" +
                "activeBuilds=" + activeBuilds +
                ", pendingBuilds=" + pendingBuilds +
                ", historyBuilds=" + historyBuilds +
                '}';
    }
}
