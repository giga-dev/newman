package com.gigaspaces.newman.report.daily;

/**
 * Created by moran on 8/19/15.
 */
class SuiteDiffSummary {
    int totalIncreasingDiff;
    int totalDecreasingDiff;

    //
    // reflection methods used by org.antlr.stringtemplate (see daily_report.st files)
    //

    public int getTotalIncreasingDiff() {
        return totalIncreasingDiff;
    }

    public int getTotalDecreasingDiff() {
        return totalDecreasingDiff;
    }

    public boolean isIncreasingDiff() {
        return totalIncreasingDiff > 0;
    }

    public boolean isDecreasingDiff() {
        return totalDecreasingDiff < 0;
    }
}
