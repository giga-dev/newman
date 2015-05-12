package com.gigaspaces.newman.beans.criteria;

import com.gigaspaces.newman.utils.StringUtils;

/**
 * Key-Value matching criteria
 */
public class PropertyCriteria implements Criteria {

    private String key;
    private String value;

    public PropertyCriteria() {
    }

    public PropertyCriteria(String key, String value) {
        this.key = StringUtils.dotToDash(key); //replacing due to mongo impl - map keys can't contain dots
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = StringUtils.dotToDash(key);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
