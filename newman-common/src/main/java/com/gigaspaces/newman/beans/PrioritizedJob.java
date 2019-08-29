package com.gigaspaces.newman.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.*;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@Entity
public class PrioritizedJob {
    @Id
    private String id;
    private String jobId;
    @Embedded
    private Set<String> agentGroups = Collections.emptySet();
    @Embedded
    private Set<String> requirements = Collections.emptySet();
    @Embedded
    private int priority;
    private boolean isPaused;

    public PrioritizedJob(){
    }

    public PrioritizedJob(Job job){
        this.jobId = job.getId();
        this.agentGroups = job.getAgentGroups();
        this.requirements = job.getSuite().getRequirements();
        this.priority = job.getPriority();
        this.isPaused = job.getState().equals(State.PAUSED);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getAgentGroups() {
        return agentGroups;
    }

    public void setAgentGroups(Set<String> agentGroups) {
        this.agentGroups = agentGroups;
    }

    public Set<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Set<String> requirements) {
        this.requirements = requirements;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getJob() {
        return jobId;
    }

    public void setJob(String jobID) {
        this.jobId = jobID;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }


    @Override
    public String toString() {
        return "PrioritizedJob{" +
                "id='" + id + '\'' +
                ", jobId='" + jobId + '\'' +
                ", agentGroups=" + agentGroups +
                ", requirements=" + requirements +
                ", priority=" + priority +
                ", isPaused=" + isPaused +
                '}';
    }
}