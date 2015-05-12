package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Build {
    @Id
    private String id;
    private String name;
    private Map<String, String> shas;
    private String branch;
    private Collection<URI> resources;
    private Collection<URI> testsMetadata; //JSON metadata of the tests
    private Date buildTime;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getShas() {
        return shas;
    }

    public void setShas(Map<String, String> shas) {
        this.shas = shas;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Collection<URI> getResources() {
        return resources;
    }

    public void setResources(Collection<URI> resources) {
        this.resources = resources;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(Date buildTime) {
        this.buildTime = buildTime;
    }

    public Collection<URI> getTestsMetadata() {
        return testsMetadata;
    }

    public void setTestsMetadata(Collection<URI> testsMetadata) {
        this.testsMetadata = testsMetadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .append("shas", shas)
                .append("branch", branch)
                .append("resources", resources)
                .append("testsMetadata", testsMetadata)
                .append("buildTime", buildTime)
                .toString();
    }
}
