package com.gigaspaces.newman.projections;

import java.util.Date;
import java.util.List;

public interface PTest {
    String getId();
    String getJobId();
    String getName();
    List<String> getArguments();
    String getStatus();
    String getErrorMessage();
    Double getTestScore();
    String getHistoryStats();
    String getAssignedAgent();
    String getTestType();
    String getAgentGroup();
    Date getStartTime();
    Date getEndTime();
    Integer getProgressPercent();
    Integer getRunNumber();
}
