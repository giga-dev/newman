package com.gigaspaces.newman.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.*;

import java.net.URI;
import java.util.Date;
import java.util.Set;

@Entity
//@JsonIgnoreProperties(ignoreUnknown = true)
public class PrioritizedJob {
    @Id
    private String id;
    @Reference
    private Job job;
    private Set<String> agentGroups;
    private Set<String> requirments;
    private int priority;
    private Set<String> preparingAgents; //Todo- it's changes dynamically

    public PrioritizedJob(Job job){
        this.job = job;
        this.agentGroups = job.getAgentGroups();
        this.requirments = job.getSuite().getRequirements();
        this.priority = job.getPriority();
        this.preparingAgents = job.getPreparingAgents();
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

    public Set<String> getRequirments() {
        return requirments;
    }

    public void setRequirments(Set<String> requirments) {
        this.requirments = requirments;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Set<String> getPreparingAgents() {
        return preparingAgents;
    }

    public void setPreparingAgents(Set<String> preparingAgents) {
        this.preparingAgents = preparingAgents;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
}