package com.gigaspaces.newman.beans;

import java.util.List;

/**
 * Created by Barak Bar Orion
 * 5/11/15.
 */
public class BuildWithJobs {
    private Build build;
    private List<Job> jobs;

    public BuildWithJobs() {
    }

    public BuildWithJobs( Build build, List<Job> jobs ) {
        this.build = build;
        this.jobs = jobs;
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

    @Override
    public String toString() {
        return "BuildWithJobs{" +
                "jobs=" + jobs +
                ", build=" + build +
                '}';
    }
}
