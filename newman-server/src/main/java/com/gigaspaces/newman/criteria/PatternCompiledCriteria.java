package com.gigaspaces.newman.criteria;

import com.gigaspaces.newman.beans.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by moran on 5/4/15.
 */
public class PatternCompiledCriteria implements CompiledCriteria {
    private Pattern pattern;

    public PatternCompiledCriteria(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean accept(Test test) {
        Matcher m = pattern.matcher(test.getName());
        boolean b = m.matches();
        return b;
    }
}
