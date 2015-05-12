package com.gigaspaces.newman.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 5/11/15.
 */
public class BuildWithJobs {
    private List<Job> jobs;
    private Build build;
    private State state;

    public BuildWithJobs() {
    }

    public BuildWithJobs(Map<String, List<Job>> groups) {

    }

    public BuildWithJobs(Build build) {
        this.build = build;
        jobs = new ArrayList<>();
        state = State.DONE;
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
        return "BuildWithJobs{" +
                "jobs=" + jobs +
                ", build=" + build +
                ", state=" + state +
                '}';
    }
}
