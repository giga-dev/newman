package com.gigaspaces.newman.criteria;

import com.gigaspaces.newman.beans.Test;

import java.util.regex.Pattern;

/**
 * Created by moran
 * on 5/4/15.
 */
public class PatternCompiledCriteria implements CompiledCriteria {
    private Pattern pattern;

    public PatternCompiledCriteria(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean accept(Test test) {
        return pattern.matcher(test.getName()).matches();
    }
}
