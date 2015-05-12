package com.gigaspaces.newman.beans.criteria;

/**
 * A test type criteria for matching
 */
public class TypeCriteria implements Criteria {

    private String type;

    public TypeCriteria() {
    }

    public TypeCriteria(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
