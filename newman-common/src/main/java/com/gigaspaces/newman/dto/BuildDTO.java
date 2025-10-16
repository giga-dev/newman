package com.gigaspaces.newman.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.entities.Build;
import com.gigaspaces.newman.projections.PBuildForView;
import com.gigaspaces.newman.utils.ConvertUtils;

import java.net.URI;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuildDTO {

    private String id;
    private String name;
    private String branch;
    private Date buildTime;

    private Map<String, String> shas;

    private Collection<URI> resources;
    private Collection<URI> testsMetadata;
    private Set<String> tags;
    private BuildStatusDTO buildStatus;

    public BuildDTO() {
    }

    public BuildDTO(PBuildForView projection) {
        this.id = projection.getId();
        this.name = projection.getName();
        this.branch = projection.getBranch();
        this.buildTime = projection.getBuildTime();
        this.tags = projection.getTags();
        this.buildStatus = new BuildStatusDTO(
                projection.getBuildStatus().getPassedTests(),
                projection.getBuildStatus().getFailedTests(),
                projection.getBuildStatus().getFailed3TimesTests() );
    }

    // Constructor
    public BuildDTO(String id, String name, String branch, Date buildTime,
                    Map<String, String> shas, Collection<URI> resources,
                    Collection<URI> testsMetadata, Set<String> tags,
                    BuildStatusDTO buildStatus) {
        this.id = id;
        this.name = name;
        this.branch = branch;
        this.buildTime = buildTime;
        this.shas = shas;
        this.resources = ConvertUtils.unpackPersistentBag(resources);
        this.testsMetadata = ConvertUtils.unpackPersistentBag(testsMetadata);
        this.tags = tags;
        this.buildStatus = buildStatus;
    }

    // Getters and setters

    // Conversion method for Build entity to DTO
    public static BuildDTO fromEntity(Build build) {
        BuildStatusDTO buildStatusDTO = build.getBuildStatus() != null
                ? BuildStatusDTO.fromEntity(build.getBuildStatus())
                : null;

        return new BuildDTO(
                build.getId(),
                build.getName(),
                build.getBranch(),
                build.getBuildTime(),
                build.getShas(),
                build.getResources(),
                build.getTestsMetadata(),
                build.getTags(),
                buildStatusDTO
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Date getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(Date buildTime) {
        this.buildTime = buildTime;
    }

    public Map<String, String> getShas() {
        return shas;
    }

    public void setShas(Map<String, String> shas) {
        this.shas = shas;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public BuildStatusDTO getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(BuildStatusDTO buildStatus) {
        this.buildStatus = buildStatus;
    }

    public Collection<URI> getResources() {
        return resources;
    }

    public void setResources(Collection<URI> resources) {
        this.resources = resources;
    }

    public Collection<URI> getTestsMetadata() {
        return testsMetadata;
    }

    public void setTestsMetadata(Collection<URI> testsMetadata) {
        this.testsMetadata = testsMetadata;
    }
}
