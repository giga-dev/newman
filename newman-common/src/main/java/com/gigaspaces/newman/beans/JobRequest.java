package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

import java.util.Set;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
public class JobRequest {

    private String buildId;
    private String suiteId;
    private String configId;
    private String author;
    private Set<String> agentGroups;
    private  int priority;

    public JobRequest() {
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public void setAgentGroups(Set<String> agentGroups) { this.agentGroups = agentGroups; }

    public Set<String> getAgentGroups() { return agentGroups; }

    public int getPriority() { return priority; }

    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("buildId", buildId)
                .append("suiteId", suiteId)
                .append("configId", configId)
                .append("author", author)
                .append("agentGroups", agentGroups)
                .append("priority", priority)
                .toString();
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
