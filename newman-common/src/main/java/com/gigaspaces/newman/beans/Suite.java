package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.beans.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.*;

/**
 * Created by moran on 4/29/15.
 */
@Entity
public class Suite {
    @Id
    private String id;
    private List<String> tests;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getTests() {
        return tests;
    }

    public void setTests(List<String> tests) {
        this.tests = tests;
    }


    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("tests", tests)
                .toString();
    }
}
