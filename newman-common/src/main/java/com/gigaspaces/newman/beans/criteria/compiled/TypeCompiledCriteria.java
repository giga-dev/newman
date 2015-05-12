package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.beans.Test;

public class TypeCompiledCriteria implements CompiledCriteria {

    private final String testType;

    public TypeCompiledCriteria(String testType) {
        this.testType = testType;
    }

    @Override
    public boolean accept(Test test) {
        return testType.equals(test.getTestType());
    }
}
