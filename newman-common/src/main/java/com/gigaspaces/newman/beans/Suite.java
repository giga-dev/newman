package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by moran
 * on 4/29/15.
 */
@Entity
public class Suite {

    public static final String THREADS_LIMIT = "THREADS_LIMIT";
    public static final String CUSTOM_SETUP_TIMEOUT = "CUSTOM_SETUP_TIMEOUT"; // in milliseconds

    @Id
    private String id;

    private String name;

    private String customVariables; //key=value separated by comma, e.g SUITE_TYPE=tgrid,SUPPORT_SGTEST=sgtest

    @Embedded
    private Criteria criteria;

    public Suite() {
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

    public static Map<String,String> parseCustomVariables(String customVariables){
        Map<String,String> res = new HashMap<>();
        if (customVariables != null) {
            for (String variableKeyValue : customVariables.split(",")) {
                res.put(variableKeyValue.split("=")[0], variableKeyValue.split("=")[1]);
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .append("custom environment variables", customVariables)
                .append("criteria", criteria)
                .toString();
    }
}
