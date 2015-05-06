package com.gigaspaces.newman.criteria;

import com.gigaspaces.newman.beans.Test;

import java.util.List;
import java.util.Optional;

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
