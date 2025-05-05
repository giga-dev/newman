package com.gigaspaces.newman.crons.suitediff;

import com.gigaspaces.newman.entities.Suite;

/**
 * Created by moran on 8/19/15.
 */
class DiffComparableData implements Comparable<DiffComparableData> {
    Suite suite;
    int failedTests;
    int diffFailedTests;
    int totalTests;
    int diffTotalTests;

    @Override
    public int compareTo(DiffComparableData o) {

        int compareToResult = 0;

        //compare failures as equal disregarding diffs
        compareToResult = Integer.valueOf(failedTests).compareTo(Integer.valueOf(o.failedTests));
        if (compareToResult != 0) {
            return -1 * compareToResult;
        }

        //compare failures as equal disregarding number of tests in suite
        compareToResult = Integer.valueOf(failedTests + diffFailedTests).compareTo(Integer.valueOf(o.failedTests + o.diffFailedTests));
        if (compareToResult != 0) {
            return -1 * compareToResult;
        }

        //compare failed+diff divided by total+diff (ratio of failures)
        double thisFailures = Double.valueOf(failedTests + diffFailedTests).doubleValue() / Double.valueOf(totalTests + diffTotalTests).doubleValue();
        double otherFailures = Double.valueOf(o.failedTests + o.diffFailedTests).doubleValue() / Double.valueOf(o.totalTests + o.diffTotalTests).doubleValue();

        compareToResult = Double.valueOf(thisFailures).compareTo(Double.valueOf(otherFailures));
        if (compareToResult != 0) {
            return -1 * compareToResult;
        }

        //never return 0 when using TreeSet
        return -1;
    }

    //
    // reflection methods used by org.antlr.stringtemplate (see body-template.st files)
    //

    public String getSuiteName() {
        return suite.getName();
    }

    public int getFailedTests() {
        return failedTests;
    }

    public int getDiffFailedTests() {
        return diffFailedTests;
    }

    public boolean isIncreasingDiffFailedTests() {
        return diffFailedTests > 0;
    }

    public boolean isDecreasingDiffFailedTests() {
        return diffFailedTests < 0;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public int getDiffTotalTests() {
        return diffTotalTests;
    }

    public boolean isIncreasingDiffTotalTests() {
        return diffTotalTests > 0;
    }

    public boolean isDecreasingDiffTotalTests() {
        return diffTotalTests < 0;
    }
}
