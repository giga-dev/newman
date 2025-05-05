package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.entities.Test;

public class TestCompiledCriteria implements CompiledCriteria {

    private final Test test;

    public TestCompiledCriteria(Test test) {
        this.test = test;
    }

    @Override
    public boolean accept(Test other) {
        if (test.getName() != null) {
            if (other.getName() == null || !test.getName().equals(other.getName())) {
                return false;
            }
        }

        if (test.getTestType() != null) {
            if (other.getTestType() == null || !test.getTestType().equals(other.getTestType())) {
                return false;
            }
        }

        if (test.getArguments() != null) {
            if (other.getArguments() == null) {
                return false;
            }
            for (String arg : test.getArguments()) {
                if (!other.getArguments().contains(arg)) {
                    return false;
                }
            }
        }

        return true;
    }
}
