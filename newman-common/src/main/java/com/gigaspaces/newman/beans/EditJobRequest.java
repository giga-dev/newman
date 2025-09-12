package com.gigaspaces.newman.beans;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Set;

public class EditJobRequest {

    private Set<String> agentGroups;
    private int priority;

    public EditJobRequest() {
    }

    public Set<String> getAgentGroups() {
        return agentGroups;
    }

    public void setAgentGroups(Set<String> agentGroups) {
        this.agentGroups = agentGroups;
    }

    public int getPriority() { return priority; }

    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("agentGroups", agentGroups)
                .append("priority", priority)
                .toString();
    }
}
