package com.gigaspaces.newman;

import com.gigaspaces.newman.utils.ToStringBuilder;

import java.util.List;

/**
 * @author Yael Nahon
 * @since 12.3
 */
public class FutureJobsRequest {

    private String buildId;
    private String configId;
    private List<String> suites;

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

    public void setSuiteId( List<String> suites) {
        this.suites = suites;
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
                .append("suites", suites)
                .append("configId",configId)
                .toString();
    }
}
