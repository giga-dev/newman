package com.gigaspaces.newman.projections;

import java.util.Date;

public interface PTestForHistory {
    String getName();
    Date getEndTime();
    String getArguments();
    String getSha();
    String getBranch();
    String getJobId();
    String getBuildId();
    String getSuiteId();
}
