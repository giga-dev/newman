package com.gigaspaces.newman.beans.criteria;

/**
 * Key-Value matching criteria
 */
public class PropertyCriteria implements Criteria {

    private String key;
    private String value;

    public PropertyCriteria() {
    }

    public PropertyCriteria(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
