package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.beans.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.Collection;

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

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("buildId", buildId)
                .toString();
    }
}
