package com.gigaspaces.newman.criteria;

import com.gigaspaces.newman.beans.Test;

import java.util.List;

/**
 * Created by moran
 * on 5/4/15.
 */
public class OrCompiledCriteria implements CompiledCriteria {
    private final List<CompiledCriteria> compiledCriteriaList;

    public OrCompiledCriteria(List<CompiledCriteria> compiledCriteriaList) {
        this.compiledCriteriaList = compiledCriteriaList;
    }

    @Override
    public boolean accept(Test test) {
        for (CompiledCriteria compiledCriteria : compiledCriteriaList) {
            if (compiledCriteria.accept(test)) {
                return true;
            }
        }
        return false;
    }
}
