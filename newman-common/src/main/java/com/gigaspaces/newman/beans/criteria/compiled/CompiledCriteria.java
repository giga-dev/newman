package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.beans.Test;

/**
 * Created by moran
 * on 5/4/15.
 */
public interface CompiledCriteria {
    boolean accept(Test test);
}
