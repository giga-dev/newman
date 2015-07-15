package com.gigaspaces.newman;

/**
 * @author Boris
 * @since 1.0
 */
public class NewmanBuildMetadata {

    public static final String BUILD_S3_PUBLISH_FOLDER = "BUILD_S3_PUBLISH_FOLDER";
    public static final String NEWMAN_BUILD_MILESTONE = "NEWMAN_BUILD_MILESTONE";
    public static final String NEWMAN_BUILD_VERSION = "NEWMAN_BUILD_VERSION";
    public static final String NEWMAN_BUILD_NUMBER = "NEWMAN_BUILD_NUMBER";
    public static final String NEWMAN_BUILD_BRANCH = "NEWMAN_BUILD_BRANCH";

    private String publishFolder;
    private String newmanBuildMilestone;
    private String newmanBuildVersion;
    private String buildBranch;
    private String buildNumber;

    public NewmanBuildMetadata(String publishFolder, String newmanBuildMilestone, String newmanBuildVersion, String buildBranch, String buildNumber) {
        this.publishFolder = publishFolder;
        this.newmanBuildMilestone = newmanBuildMilestone;
        this.newmanBuildVersion = newmanBuildVersion;
        this.buildBranch = buildBranch;
        this.buildNumber = buildNumber;
    }

    public String getPublishFolder() {
        return publishFolder;
    }

    public String getNewmanBuildMilestone() {
        return newmanBuildMilestone;
    }

    public String getNewmanBuildVersion() {
        return newmanBuildVersion;
    }

    public String getBuildBranch() {
        return buildBranch;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

}
