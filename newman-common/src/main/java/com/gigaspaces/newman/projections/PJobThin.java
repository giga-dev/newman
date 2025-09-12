package com.gigaspaces.newman.projections;

public interface PJobThin {
    String getId();
    String getSuiteId();
    String getSuiteName();
    String getBuildId();
    String getBuildName();
    String getBuildBranch();

    default String string() {
        return "Job[" +
               "id=" + getId() +
               ", suiteId=" + getSuiteId() +
               ", suiteName=" + getSuiteName() +
               ", buildId=" + getBuildId() +
               ", buildName=" + getBuildName() +
               ", buildBranch=" + getBuildBranch() +
               "]";
    }
}
