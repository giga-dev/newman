package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.utils.IndexDirection;

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
    @Indexed(value= IndexDirection.ASC, unique=false)
    private String name;
    private Map<String, String> shas;
    @Indexed(value= IndexDirection.ASC, unique=false)
    private String branch;
    private Collection<URI> resources;
    private Collection<URI> testsMetadata; //JSON metadata of the tests
    private Set<String> tags;
    private Date buildTime;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag){
        tags.add(tag);
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

        if (id != null ? !id.equals(build.id) : build.id != null) return false;
        if (name != null ? !name.equals(build.name) : build.name != null) return false;
        return !(branch != null ? !branch.equals(build.branch) : build.branch != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        return result;
    }
}
