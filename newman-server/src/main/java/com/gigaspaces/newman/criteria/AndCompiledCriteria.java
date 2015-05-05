package com.gigaspaces.newman.criteria;

import com.gigaspaces.newman.beans.Test;

import java.util.List;

/**
 * Created by moran on 5/4/15.
 */
public class AndCompiledCriteria implements CompiledCriteria {
    private final List<CompiledCriteria> compiledCriterias;

    public AndCompiledCriteria(List<CompiledCriteria> compiledCriterias) {
        this.compiledCriterias = compiledCriterias;
    }

    @Override
    public boolean accept(Test test) {
        for (CompiledCriteria compiledCriteria : compiledCriterias) {
            if (!compiledCriteria.accept(test)) {
                return false;
            }
        }
        return true;
    }
}
