package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
public class JobRequest {

    private String buildId;
    private String suiteId;
    private String configId;
    private String author;

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

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("buildId", buildId)
                .append("suiteId", suiteId)
                .append("configId", configId)
                .append("author", author)
                .toString();
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
