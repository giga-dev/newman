package com.gigaspaces.newman;

import java.util.Collection;
import java.util.Set;

/**
 * @author Boris
 * @since 1.0
 */
public class NewmanBuildMetadata {

    public static final String NEWMAN_BUILD_TESTS_METADATA = "NEWMAN_BUILD_TESTS_METADATA";
    public static final String NEWMAN_BUILD_SHAS_FILE = "NEWMAN_BUILD_SHAS_FILE";
    public static final String NEWMAN_BUILD_RESOURCES = "NEWMAN_BUILD_RESOURCES";
    public static final String NEWMAN_BUILD_NUMBER = "NEWMAN_BUILD_NUMBER";
    public static final String NEWMAN_BUILD_BRANCH = "NEWMAN_BUILD_BRANCH";
    public static final String NEWMAN_BUILD_TAGS = "NEWMAN_BUILD_TAGS";

    private String buildBranch;
    private String buildNumber;
    private String buildShasFile;
    private Collection<String> resources;
    private Collection<String> testsMetadata;
    private Set<String> tags;

    public NewmanBuildMetadata(){}

    public String getBuildBranch() {
        return buildBranch;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public String getBuildShasFile() {
        return buildShasFile;
    }

    public Collection<String> getResources() {
        return resources;
    }

    public Collection<String> getTestsMetadata() {
        return testsMetadata;
    }

    public void setBuildBranch(String buildBranch) {
        this.buildBranch = buildBranch;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public void setBuildShasFile(String buildShasFile) {
        this.buildShasFile = buildShasFile;
    }

    public void setResources(Collection<String> resources) {
        this.resources = resources;
    }

    public void setTestsMetadata(Collection<String> testsMetadata) {
        this.testsMetadata = testsMetadata;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "NewmanBuildMetadata{" +
                "buildBranch='" + buildBranch + '\'' +
                ", buildNumber='" + buildNumber + '\'' +
                ", buildShasFile='" + buildShasFile + '\'' +
                ", resources=" + resources +
                ", testsMetadata=" + testsMetadata +
                ", tags=" + tags +
                '}';
    }
}
