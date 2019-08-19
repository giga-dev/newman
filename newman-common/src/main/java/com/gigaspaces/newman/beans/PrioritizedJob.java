package com.gigaspaces.newman.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.*;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@Entity
//@JsonIgnoreProperties(ignoreUnknown = true) //Todo - decide if needed
public class PrioritizedJob {
    @Id
    private String id;
    private String jobID;
    @Embedded
    private Set<String> agentGroups = Collections.emptySet();
    @Embedded
    private Set<String> requirements = Collections.emptySet();
    @Embedded
    private int priority;
    @Embedded
    private int preparingAgents;

    public PrioritizedJob(){
    }

    public PrioritizedJob(Job job){
        this.jobID = job.getId();
        this.agentGroups = job.getAgentGroups();
        this.requirements = job.getSuite().getRequirements();
        this.priority = job.getPriority();
        this.preparingAgents = job.getPreparingAgents().size();
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

    public int getPreparingAgents() {
        return preparingAgents;
    }

    public void setPreparingAgents(int preparingAgents) {
        this.preparingAgents = preparingAgents;
    }

    public String getJob() {
        return jobID;
    }

    public void setJob(String jobID) {
        this.jobID = jobID;
    }
}