package com.gigaspaces.newman.entities;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gigaspaces.newman.converters.UriCollectionConverter;
import com.gigaspaces.newman.converters.MapToJsonConverter;
import javax.persistence.*;

import com.gigaspaces.newman.utils.ConvertUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.net.URI;
import java.util.*;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
@Table(name = "build", indexes = {
        @Index(name = "idx_suite_name", columnList = "name"),
        @Index(name = "idx_suite_branch", columnList = "branch")
})
public class Build {

    @Id
    private String id;
    private String name;
    private String branch;
    private Date buildTime;

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> shas;

    @Fetch(FetchMode.SUBSELECT)
    @Convert(converter = UriCollectionConverter.class)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "build_resources", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "resources", columnDefinition = "TEXT[]")
    private Collection<URI> resources;

    @Fetch(FetchMode.SUBSELECT)
    @Convert(converter = UriCollectionConverter.class)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "build_tests_metadata", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "tests_metadata", columnDefinition = "TEXT[]")
    private Collection<URI> testsMetadata; //JSON metadata of the tests

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "build_tags", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "tags")
    private Set<String> tags;

    @OneToOne(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
    private BuildStatus buildStatus;

    public Build() {
        this.shas = new HashMap<>();
        this.resources = new ArrayList<>();
        this.testsMetadata = new ArrayList<>();
        this.tags = new HashSet<>();
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
        return ConvertUtils.unpackPersistentBag(resources);
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
        return ConvertUtils.unpackPersistentBag(this.testsMetadata);
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag){
        tags.add(tag);
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("shas", shas)
                .append("branch", branch)
                .append("resources", resources)
                .append("testsMetadata", testsMetadata)
                .append("tags", tags)
                .append("buildTime", buildTime)
                .append("buildStatus", buildStatus)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Build build = (Build) o;
        return id.equals(build.id) && name.equals(build.name) && branch.equals(build.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, branch);
    }

    public void excludeFields() {
        this.shas = new HashMap<>();
        this.resources = new ArrayList<>();
        this.testsMetadata = new ArrayList<>();
        this.tags = new HashSet<>();
    }
}
