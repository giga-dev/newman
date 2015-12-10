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

    private Suite suite;

    private String name;

    private List<Job> jobs = new ArrayList<Job>();

    public SuiteWithJobs() {
    }

    public SuiteWithJobs( Suite suite ) {
        this(suite, Collections.emptyList());
    }

    public SuiteWithJobs(Suite suite, List<Job> jobsList) {
        this.suite = suite;
        this.jobs = jobsList;
        for( Job job : this.jobs ){
            job.setBuild( job.getBuild() );
            job.setBuild( null );
//            job.setSuite( job.getSuite() );
            job.setSuite( null );
        }
        //we don't have to display following properties them we set null values in order to decrease passed json size
        suite.setCriteria(null);
        suite.setCustomVariables(null);
        suite.setRequirements(null);
        suite.setDisplayedCriteria(null);

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

    public Suite getSuite() {
        return suite;
    }

    public void setSuite(Suite suite) {
        this.suite = suite;
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
