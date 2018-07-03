package com.gigaspaces.newman.beans;

public enum JavaVersion {
    VERSION7("7"),VERSION8("8"),VERSION9("9");

    private String name;

    JavaVersion(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
