package com.gigaspaces.newman.projections;

import java.util.Date;
import java.util.Set;

public interface PBuildForView {
    String getId();
    String getName();
    String getBranch();
    Date getBuildTime();
    Set<String> getTags();
    PBuildForView.BuildStatus getBuildStatus();

    interface BuildStatus {
        int getPassedTests();
        int getFailedTests();
        int getFailed3TimesTests();
    }
}
