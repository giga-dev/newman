package com.gigaspaces.newman.crons.suitediff;

import com.gigaspaces.newman.entities.Suite;
import com.gigaspaces.newman.entities.Test;

/**
 * Created by moran on 5/14/17.
 */
public class HistoryTestData {
    private Test test;
    private Suite suite;
    private String testURL;

    public HistoryTestData(Test test, Suite suite, String testURL) {
        this.test = test;
        this.suite = suite;
        this.testURL = testURL;
    }

    //
    // reflection methods used by org.antlr.stringtemplate (see body-template.st files)
    //
    public String getTestName() {
        return test.getName();
    }

    public String getTestURL() {
        return testURL;
    }

    public String getSuiteName() {
        return suite.getName();
    }
}
