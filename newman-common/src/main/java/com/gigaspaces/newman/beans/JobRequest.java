package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.Collection;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
public class JobRequest {
    @Id
    private String id;
    private String buildId;
    private Collection<URI> resources;

    public JobRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public Collection<URI> getResources() {
        return resources;
    }

    public void setResources(Collection<URI> resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        return "JobRequest{" +
                "id='" + id + '\'' +
                ", buildId='" + buildId + '\'' +
                ", resources=" + resources +
                '}';
    }
}
