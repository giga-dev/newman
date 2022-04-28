package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.beans.Test;

import java.util.List;

public class SuiteCompiledCriteria implements CompiledCriteria {
    private String type;
    private final List<CompiledCriteria> include;
    private final List<CompiledCriteria> exclude;

    public SuiteCompiledCriteria(List<CompiledCriteria> include, List<CompiledCriteria> exclude, String type) {
        this.exclude = exclude;
        this.include = include;
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public List<CompiledCriteria> getExclude() {
        return exclude;
    }

    public List<CompiledCriteria> getInclude() {
        return include;
    }

    @Override
    public boolean accept(Test test) {
         if (this.type != null && !this.type.equals(test.getTestType()))
            return false;

        for (CompiledCriteria criteria : exclude) {
            if (criteria.accept(test))
                return false;
        }

        for(CompiledCriteria criteria : include) {
            if (criteria.accept(test)) {
                return true;
            }
        }

        if (include.isEmpty())
            return true;

        return false;
    }



}
