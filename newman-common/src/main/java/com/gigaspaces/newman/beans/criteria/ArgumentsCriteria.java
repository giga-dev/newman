package com.gigaspaces.newman.beans.criteria;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tamirs
 * on 10/6/15.
 *
 * ** IMPORTANT ** accept using contains, NOT equality of argument!
 *    e.g critria = remote.
 *        args = remote&embaded.
 *        will find a match!
 * for equality use {@link TestCriteria#createCriteriaByTestArgs(String...)}
 */
public class ArgumentsCriteria implements Criteria {
    private String arg;

    public ArgumentsCriteria(){
    }

    public ArgumentsCriteria(String arg){
        this.arg = arg;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public static ArgumentsCriteria containsCriteria(String testArg) {
        return new ArgumentsCriteria(testArg);
    }


    public static AndCriteria containsAllCriteria(String ... testArgs) {
        List<Criteria> argumentsCriteriaList = new ArrayList<>();
        for (String testArg : testArgs) {
            argumentsCriteriaList.add(new ArgumentsCriteria(testArg));
        }
        AndCriteria ac = new AndCriteria();
        ac.setCriterias(argumentsCriteriaList);
        return ac;
    }

    public static Criteria containsEitherCriteria(String ... testArgs) {
        List<Criteria> argumentsCriteriaList = new ArrayList<>();
        for (String testArg : testArgs) {
            argumentsCriteriaList.add(new ArgumentsCriteria(testArg));
        }
        OrCriteria oc = new OrCriteria();
        oc.setCriterias(argumentsCriteriaList);
        return oc;
    }

    @Override
    public  String toString(){
        return new ToStringBuilder(this)
                .append("arg", arg)
                .toString();
    }
}
