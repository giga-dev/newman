package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.*;

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
    private BuildStatus buildStatus;
    private List<String> suitesNames;
    private List<String> suitesIds;

    public Build() {
        this.suitesNames = new ArrayList<>();
        this.suitesIds = new ArrayList<>();
        this.shas = new HashMap<>();
        this.resources = new ArrayList<>();
        this.testsMetadata = new ArrayList<>();
    }

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

    public BuildStatus getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(BuildStatus buildStatus) {
        this.buildStatus = buildStatus;
    }

    public List<String> getSuitesNames() {
        return suitesNames;
    }

    public void setSuitesNames(List<String> suitesNames) {
        this.suitesNames = suitesNames;
    }

    public List<String> getSuitesIds() {
        return suitesIds;
    }

    public void setSuitesIds(List<String> suitesIds) {
        this.suitesIds = suitesIds;
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
                .append("buildStatus", buildStatus)
                .append("suitesNames", suitesNames)
                .append("suitesIds", suitesIds)
                .toString();
    }
}
