package com.gigaspaces.newman.entities;


import com.gigaspaces.newman.beans.State;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "prioritized_job")
public class PrioritizedJob {
    @Id
    private String id;
    private String jobId;

    private int priority;
    private boolean isPaused;

    @Type(type = "com.gigaspaces.newman.types.SetStringArrayType")
    @Column(name = "agent_groups", columnDefinition = "TEXT[]")
    private Set<String> agentGroups;

    @Type(type = "com.gigaspaces.newman.types.SetStringArrayType")
    @Column(name = "requirements", columnDefinition = "TEXT[]")
    private Set<String> requirements;

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

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
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