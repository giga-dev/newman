package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.beans.Test;

import java.util.List;

/**
 * Created by moran
 * on 5/4/15.
 */
public class SpecificTestsCompiledCriteria implements CompiledCriteria {


    private final List<String> testNames;

    public SpecificTestsCompiledCriteria(List<String> testNames) {
        this.testNames = testNames;
    }

    @Override
    public boolean accept(Test test) {
        for (String testName : testNames) {
            if (testName.equals(test.getName())) {
                return true;
            }
        }
        return false;
    }
}
