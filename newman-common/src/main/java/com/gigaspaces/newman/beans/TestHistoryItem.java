package com.gigaspaces.newman.beans;

/**
 * Created by evgenyf
 * on 7/6/2015.
 */
public class TestHistoryItem {

    private Test test;
    private Job job;

    public TestHistoryItem(){

    }

    public TestHistoryItem( Test test, Job job ){
        this.test = test;
        this.job = job;
    }

    public Test getTest() {
        return test;
    }

    public Job getJob() {
        return job;
    }
}