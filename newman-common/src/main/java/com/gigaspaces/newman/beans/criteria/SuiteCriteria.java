package com.gigaspaces.newman.beans.criteria;
import java.util.Collections;
import java.util.List;

public class SuiteCriteria implements Criteria {
    private String suiteType;
    private List<Criteria> include;
    private List<Criteria> exclude;

    @SuppressWarnings("Called by json construction to avoid null lists")
    public SuiteCriteria() {
        include = Collections.emptyList();
        exclude = Collections.emptyList();
    }


    public SuiteCriteria(List<Criteria> include, List<Criteria> exclude, String type) {
        this.include = include;
        this.exclude = exclude;
        this.suiteType = type;
    }

    public String getSuiteType() {
        return suiteType;
    }

    public void setSuiteType(String suiteType) {
        this.suiteType = suiteType;
    }

    public List<Criteria> getInclude(){
        return this.include;}

    public void setExclude(List<Criteria> exclude) {
        this.exclude = exclude;
    }

    public void setInclude(List<Criteria> include) {
        this.include = include;
    }

    public List<Criteria> getExclude(){
        return this.exclude;}

    @Override
    public String toString() {
        return "SuiteCriteria{" +
                "suiteType='" + suiteType + '\'' +
                ", include=" + include +
                ", exclude=" + exclude +
                '}';
    }
}
