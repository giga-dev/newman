package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.entities.Job;
import com.gigaspaces.newman.projections.PSuiteThin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author evgenyf
 * 6/9/15.
 */
public class SuiteWithJobs {

    private String id;

    private SuiteView suite;

    private String name;

    private List<JobView> jobs = new ArrayList<JobView>();

    public SuiteWithJobs() {
    }

    public SuiteWithJobs(PSuiteThin suite, List<Job> jobsList) {
        prepareJobViews(jobsList);
        setId(suite.getId());
        setName( suite.getName() );
        this.suite = new SuiteView( suite );
    }

    private void prepareJobViews(List<Job> jobsList) {
        this.jobs = new ArrayList<>( jobsList.size() );
        for( Job job : jobsList ){
            jobs.add( new JobView( job ) );
        }
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
