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

    private SuiteView suite;

    private String name;

    private List<JobView> jobs = new ArrayList<JobView>();

    public SuiteWithJobs() {
    }

    public SuiteWithJobs( Suite suite ) {
        this(suite, Collections.emptyList());
    }

    public SuiteWithJobs(Suite suite, List<Job> jobsList) {
        this.jobs = new ArrayList<>( jobsList.size() );
        for( Job job : jobsList ){
            jobs.add( new JobView( job ) );
        }
        setId(suite.getId());
        setName( suite.getName() );
        this.suite = new SuiteView( suite );
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

    public List<JobView> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobView> jobs) {
        this.jobs = jobs;
    }

    public SuiteView getSuite() {
        return suite;
    }

    public void setSuite(SuiteView suite) {
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
