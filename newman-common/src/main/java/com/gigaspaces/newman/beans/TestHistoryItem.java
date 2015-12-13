package com.gigaspaces.newman.beans;

/**
 * Created by evgenyf
 * on 7/6/2015.
 */
public class TestHistoryItem {

    private TestView test;
    private JobView job;

    public TestHistoryItem(){

    }

    public TestHistoryItem( TestView test, JobView job ){
        this.test = test;
        this.job = job;
    }

    public TestView getTest() {
        return test;
    }

    public JobView getJob() {
        return job;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TestHistoryItem{");
        sb.append("test=").append(test);
        sb.append(", job=").append(job);
        sb.append('}');
        return sb.toString();
    }
}