package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.entities.Test;

/**
 * Created by moran
 * on 5/5/15.
 */
public class NotCompiledCriteria implements CompiledCriteria {

    private final CompiledCriteria compiledCriteria;

    public NotCompiledCriteria(CompiledCriteria compiledCriteria) {
        this.compiledCriteria = compiledCriteria;
    }

    @Override
    public boolean accept(Test test) {
        return !compiledCriteria.accept(test);
    }
}
