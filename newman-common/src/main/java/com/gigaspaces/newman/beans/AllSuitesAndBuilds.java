package com.gigaspaces.newman.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgeny Fisher
 * 2/25/16.
 */
public class AllSuitesAndBuilds {

    private List<SuiteView> suites = new ArrayList<SuiteView>();
    private List<BuildView> builds = new ArrayList<BuildView>();

    public AllSuitesAndBuilds() {
    }

    public AllSuitesAndBuilds( List<BuildView> builds, List<SuiteView> suites ) {
        this.builds = builds;
        this.suites = suites;
    }

    public List<SuiteView> getSuites() {
        return suites;
    }

    public List<BuildView> getBuilds() {
        return builds;
    }
}
