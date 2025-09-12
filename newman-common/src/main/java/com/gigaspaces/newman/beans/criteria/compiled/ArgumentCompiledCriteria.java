package com.gigaspaces.newman.beans.criteria.compiled;

import com.gigaspaces.newman.entities.Test;

import java.util.List;

/**
 * Created by tamirs
 * on 10/6/15.
 */

public class ArgumentCompiledCriteria implements CompiledCriteria{

    private final String arg;

    public ArgumentCompiledCriteria(String arg) {
        this.arg = arg;
    }

    @Override
    public boolean accept(Test other) {
        String OtherArgumentsAsString = ListToString(other.getArguments());

        return OtherArgumentsAsString.contains(arg);
    }

    private String ListToString(List args){
        return String.join(" ", args);
    }
}
