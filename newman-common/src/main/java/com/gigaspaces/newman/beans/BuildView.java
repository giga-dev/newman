package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;

/**
 * Created by Evgeny Fisher
 * 2/25/16.
 */
public class BuildView {
    private String id;
    private String name;
    private String branch;

    public BuildView( Build build ) {
        this.id = build.getId();
        this.name = build.getName();
        this.branch = build.getBranch();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .toString();
    }
}