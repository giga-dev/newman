package com.gigaspaces.newman.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gigaspaces.newman.NewmanConsts;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobConfig {

    @Id
    private String id;

    private String name;
    private JavaVersion javaVersion;

    // the constructor will define
    // default for each configuration
    public JobConfig() {
        name= NewmanConsts.DEFAULT_CONFIG_NAME;
        javaVersion=NewmanConsts.DEFAULT_JAVA_VERSION;
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

    public JavaVersion getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(JavaVersion javaVersion) {
        this.javaVersion = javaVersion;
    }

    @Override
    public String toString() {
        return "JobConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", javaVersion=" + javaVersion +
                '}';
    }
}
