package com.gigaspaces.newman;

import com.gigaspaces.newman.utils.ToStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Yael Nahon
 * @since 12.3
 */
public class FutureJobsRequest {

    private String buildId;
    private String configId;
    private List<String> suites;
    private Set<String> agentGroups = Collections.emptySet();
    private String author;

    public FutureJobsRequest() {
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public  List<String> getSuites() {
        return suites;
    }

    public void setSuites( List<String> suites) {
        this.suites = suites;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public Set<String> getAgentGroups() {
        return agentGroups;
    }

    public void setAgentGroups(Set<String> agentGroups) {
        this.agentGroups = agentGroups;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("buildId", buildId)
                .append("suites", suites)
                .append("configId",configId)
                .append("author",author)
                .append("agentGroups",agentGroups)
                .toString();
    }
}
