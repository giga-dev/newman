package com.gigaspaces.newman.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "test_logs", uniqueConstraints = @UniqueConstraint(columnNames = "test_id"))
public class TestLog {

    @Id
    private String id;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "test_id")
    private Test test;

    @Type(type = "com.gigaspaces.newman.types.MapJsonType")
    @Column(name = "logs", columnDefinition = "JSON")
    private Map<String, String> testLogs = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getTestLogs() {
        return testLogs;
    }

    public void setTestLogs(Map<String, String> testLogs) {
        this.testLogs = testLogs;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
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
                .append("testId", test.getId())
                .append("testLogs", testLogs)
                .toString();
    }
}
