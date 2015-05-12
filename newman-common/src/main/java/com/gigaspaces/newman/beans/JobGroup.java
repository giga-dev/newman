package com.gigaspaces.newman.beans;

import java.util.List;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 5/11/15.
 */
public class JobGroup {
    private List<Job> jobs;
    private Build build;
    private State state;

    public JobGroup() {
    }

    public JobGroup(Map<String, List<Job>> groups) {

    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public Build getBuild() {
        return build;
    }

    public void setBuild(Build build) {
        this.build = build;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "JobGroup{" +
                "jobs=" + jobs +
                ", build=" + build +
                ", state=" + state +
                '}';
    }
}
