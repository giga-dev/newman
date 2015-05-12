package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.beans.Test;

import java.util.List;

/**
 * Created by moran
 * on 5/4/15.
 */
public class AndCompiledCriteria implements CompiledCriteria {
    private final List<CompiledCriteria> compiledCriteriaList;

    public AndCompiledCriteria(List<CompiledCriteria> compiledCriteriaList) {
        this.compiledCriteriaList = compiledCriteriaList;
    }

    @Override
    public boolean accept(Test test) {
        for (CompiledCriteria compiledCriteria : compiledCriteriaList) {
            if (!compiledCriteria.accept(test)) {
                return false;
            }
        }
        return true;
    }
}
