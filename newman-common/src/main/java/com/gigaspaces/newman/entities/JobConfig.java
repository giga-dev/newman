package com.gigaspaces.newman.entities;

import com.gigaspaces.newman.NewmanConsts;
import com.gigaspaces.newman.beans.JavaVersion;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "job_config")
public class JobConfig {

    @Id
    private String id;

    private String name;

    @Enumerated(value = EnumType.STRING)
    private JavaVersion javaVersion;

    private boolean isDefault;

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

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("javaVersion", javaVersion)
                .append("isDefault", isDefault)
                .toString();
    }
}
