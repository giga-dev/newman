package com.gigaspaces.newman.projections;

import java.net.URI;
import java.util.Date;
import java.util.Set;

import com.gigaspaces.newman.beans.State;

public interface PJob {
    String getId();

    PJob.Build getBuild();
    PJob.Suite getSuite();
    PJob.JobConfig getJobConfig();

    Date getSubmitTime();
    Date getStartTime();
    Date getEndTime();

    URI getTestURI();
    String getSubmittedBy();
    State getState();

    int getTotalTests();
    int getPassedTests();
    int getFailedTests();
    int getFailed3TimesTests();
    int getRunningTests();
    int getNumOfTestRetries();

    Set<String> getPreparingAgents();
    Set<String> getAgentGroups();
    int getPriority();

    interface Build {
        String getId();
        String getName();
        String getBranch();
    }

    interface Suite {
        String getId();
        String getName();
    }

    interface JobConfig {
        String getId();
        String getName();
    }
}
