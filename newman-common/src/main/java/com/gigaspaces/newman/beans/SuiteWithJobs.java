package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author evgenyf
 * 6/9/15.
 */
public class SuiteWithJobs {

    @Id
    private String id;

    private String name;

    private List<Job> jobs = new ArrayList<Job>();

    public SuiteWithJobs() {
    }

    public SuiteWithJobs( Suite suite ) {
        this( suite, Collections.emptyList() );
    }

    public SuiteWithJobs(Suite suite, List<Job> jobsList) {
        this.jobs = jobsList;
        setId( suite.getId() );
        setName( suite.getName() );
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

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    @Override
    public String toString() {
        return "SuiteWithJobs{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", jobs=" + jobs +
                '}';
    }
}
