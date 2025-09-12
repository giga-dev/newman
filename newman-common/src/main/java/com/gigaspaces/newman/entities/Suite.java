package com.gigaspaces.newman.entities;

import com.gigaspaces.newman.beans.criteria.Criteria;

import javax.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

import java.util.*;

/**
 * Created by moran
 * on 4/29/15.
 */
@Entity
@Table(name = "suite", indexes = {
        @Index(name = "idx_suite_name", columnList = "name"),
        @Index(name = "idx_suite_custom_variables", columnList = "customVariables")
})
public class Suite {

    public static final String THREADS_LIMIT = "THREADS_LIMIT";
    public static final String CUSTOM_SETUP_TIMEOUT = "CUSTOM_SETUP_TIMEOUT"; // in milliseconds

    @Id
    private String id;
    private String name;        // INDEX
    @Column(length = 1024)
    private String customVariables; //INDEX; key=value separated by comma, e.g SUITE_TYPE=tgrid,SUPPORT_SGTEST=sgtest

    private Integer workersAllowed = 1;

//    @Convert(converter = StringSetConverter.class)
    @Type(type = "com.gigaspaces.newman.types.SetStringArrayType")
    @Column(name = "requirements", columnDefinition = "TEXT[]")
    private Set<String> requirements;

    @Column(name = "criteria", columnDefinition = "JSON")
//    @Convert(converter = CriteriaConverter.class)
    @Type(type = "com.gigaspaces.newman.types.CriteriaJsonType")
    private Criteria criteria;

    @Transient
    private String displayedCriteria;

    public Suite() {
        requirements = new TreeSet<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
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

    public Integer getWorkersAllowed() {
        return workersAllowed;
    }

    public static Map<String,String> parseCustomVariables(String customVariables){
        Map<String,String> res = new HashMap<>();
        if (customVariables != null) {
            for (String variableKeyValue : customVariables.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1)) {
                res.put(variableKeyValue.split("=")[0], variableKeyValue.substring(variableKeyValue.indexOf("=") + 1));
            }
        }
        return res;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }

        if (this.customVariables != null) {
            Map<String, String> vars = Suite.parseCustomVariables(this.customVariables);
            String threadsLimit = vars.get(Suite.THREADS_LIMIT);    // save threads as a separate field to use it later
            if (threadsLimit != null) {
                try {
                    this.workersAllowed = Integer.parseInt(threadsLimit);
                } catch (NumberFormatException e) {
                    this.workersAllowed = 1; // fallback
                }
            }
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("custom environment variables", customVariables)
                .append("criteria", criteria)
                .append("workers allowed", workersAllowed)
                .toString();
    }

    public String getDisplayedCriteria() {
        return displayedCriteria;
    }

    @SuppressWarnings("unused")
    public void setDisplayedCriteria(String displayedCriteria) {
        this.displayedCriteria = displayedCriteria;
    }

    public Set<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Set<String> requirements) {
        this.requirements = requirements;
    }
}
