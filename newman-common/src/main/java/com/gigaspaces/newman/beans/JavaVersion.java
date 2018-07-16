package com.gigaspaces.newman.beans;

public enum JavaVersion {
    ORACLE_7_21("oracle_7_21","oracle 1.0.7.21"), ORACLE_8_45("oracle_8_45","oracle 1.8.0.45"), ORACLE_9_0_4("oracle_9_0_4","oracle 9.0.4");

    private String name;
    private String fullVersion;

    JavaVersion(String fullVersion,String name) {
        this.name = name;
        this.fullVersion=fullVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }
}
