package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

/**
 * @author evgenyf
 * 12.13.2015
 */
public class SuiteView {

    private String id;
    private String name;
    private String customVariables;

    public SuiteView( Suite suite ) {
        id = suite.getId();
        name = suite.getName();
        customVariables = suite.getCustomVariables();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomVariables() {
        return customVariables;
    }

    public void setCustomVariables(String customVariables) {
        this.customVariables = customVariables;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .append("custom environment variables", customVariables)
                .toString();
    }
}
