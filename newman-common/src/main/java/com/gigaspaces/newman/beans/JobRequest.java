package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
public class JobRequest {

    private String buildId;
    private String suiteId;

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

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("buildId", buildId)
                .toString();
    }
}