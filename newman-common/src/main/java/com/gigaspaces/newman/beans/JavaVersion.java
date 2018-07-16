package com.gigaspaces.newman.beans;

public enum JavaVersion {
    ORACLE_7_21("oracle_7_21"), ORACLE_8_45("oracle_8_45"), ORACLE_9_0_4("oracle_9_0_4");

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
