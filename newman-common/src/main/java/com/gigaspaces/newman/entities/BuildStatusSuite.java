package com.gigaspaces.newman.entities;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;


@Entity
@Table(name = "build_status_suite")
public class BuildStatusSuite {

    @Id
    private String id;
    private String suiteId;
    private String suiteName;

    public BuildStatusSuite() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BuildStatusSuite(String suiteId, String suiteName) {
        this.suiteId = suiteId;
        this.suiteName = suiteName;
    }

    public String getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(String statusId) {
        this.suiteId = statusId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String statusName) {
        this.suiteName = statusName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildStatusSuite that = (BuildStatusSuite) o;
        return id.equals(that.id) && suiteId.equals(that.suiteId) && suiteName.equals(that.suiteName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, suiteId, suiteName);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("suiteId", suiteId)
                .append("suiteName", suiteName)
                .toString();
    }
}
