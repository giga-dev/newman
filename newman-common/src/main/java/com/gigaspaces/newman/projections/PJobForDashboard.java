package com.gigaspaces.newman.projections;

public interface PJobForDashboard {
    String getId();

    int getTotalTests();
    int getPassedTests();
    int getFailedTests();
    int getFailed3TimesTests();
    int getRunningTests();
    int getNumOfTestRetries();

    interface Suite {
        String getId();
        String getName();
    }
}
