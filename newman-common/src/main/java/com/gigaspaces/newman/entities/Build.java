package com.gigaspaces.newman.entities;

import com.gigaspaces.newman.converters.MapToJsonConverter;
import javax.persistence.*;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
@Table(name = "build", indexes = {
        @Index(name = "idx_suite_name", columnList = "name"),
        @Index(name = "idx_suite_branch", columnList = "branch")
})
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class Build {

    @Id
    private String id;
    private String name;
    private String branch;
    private Date buildTime;

    @Convert(converter = MapToJsonConverter.class)
    private Map<String, String> shas;

    @Type(type = "list-array")
    @Column(name = "resources", columnDefinition = "text[]")
    private List<String> resources;

    @Type(type = "list-array")
    @Column(name = "tests_metadata", columnDefinition = "text[]")
    private List<String> testsMetadata;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "build_tags", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "tags")
    private Set<String> tags;

    @OneToOne(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
    private BuildStatus buildStatus = new BuildStatus(this);

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

    @Transient
    public Collection<URI> getResources() {
        if (this.resources == null) return Collections.emptyList();
        return this.resources.stream()
                .map(URI::create)
                .collect(Collectors.toList());
    }

    @Transient
    public void setResources(Collection<URI> res) {
        if (res == null) {
            this.resources = new ArrayList<>();
        } else {
            this.resources = res.stream()
                    .map(URI::toString)
                    .collect(Collectors.toList());
        }
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

    @Transient
    public Collection<URI> getTestsMetadata() {
        if (this.testsMetadata == null) return new ArrayList<>();
        return this.testsMetadata.stream()
                .map(URI::create)
                .collect(Collectors.toList());
    }

    @Transient
    public void setTestsMetadata(Collection<URI> uris) {
        if (uris == null) {
            this.testsMetadata = new ArrayList<>();
        } else {
            this.testsMetadata = uris.stream()
                    .map(URI::toString)
                    .collect(Collectors.toList());
        }
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
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }

        if (buildStatus == null) {
            buildStatus = new BuildStatus(this);
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
