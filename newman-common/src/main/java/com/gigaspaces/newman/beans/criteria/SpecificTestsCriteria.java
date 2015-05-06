package com.gigaspaces.newman.beans.criteria;

import java.util.Arrays;
import java.util.List;

/**
 * A list of specific tests criteria
 */
public class SpecificTestsCriteria implements Criteria {
    private List<String> testNames;

    public SpecificTestsCriteria() {
    }

    public SpecificTestsCriteria(String ... testName) {
        testNames = Arrays.asList(testName);
    }

    public List<String> getTestNames() {
        return testNames;
    }

    public void setTestNames(List<String> testNames) {
        this.testNames = testNames;
    }
}
