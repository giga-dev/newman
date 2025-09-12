package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.entities.Test;

import java.util.Map;

/**
 * Created by moran
 * on 5/4/15.
 */
public class PropertyCompiledCriteria implements CompiledCriteria {
    private final String key;
    private final String value;

    public PropertyCompiledCriteria(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean accept(Test test) {
        Map<String, String> properties = test.getProperties();
        if (properties!=null) {
            String pvalue = properties.get(key);
            return  (value.equals(pvalue));
        }
        return false;
    }
}
