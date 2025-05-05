package com.gigaspaces.newman.projections;

import java.util.Set;

public interface PBuildThin {
    String getId();
    String getName();
    String getBranch();
    Set<String> getTags();
}
