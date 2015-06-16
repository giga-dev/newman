package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.Date;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
public class Agent {
    @Id
    private String id;
    @Indexed(unique=true, dropDups=true)
    private String name;
    private String host;
    private String jobId;
    private Date lastTouchTime;
    private String currentTest;
    private Agent.State state;

    public Agent() {
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @SuppressWarnings("unused")
    public Date getLastTouchTime() {
        return lastTouchTime;
    }

    public void setLastTouchTime(Date lastTouchTime) {
        this.lastTouchTime = lastTouchTime;
    }

    @SuppressWarnings("unused")
    public String getCurrentTest() {
        return currentTest;
    }

    @SuppressWarnings("unused")
    public void setCurrentTest(String currentTest) {
        this.currentTest = currentTest;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .append("state", state)
                .append("host", host)
                .append("jobId", jobId)
                .append("currentTest", currentTest)
                .append("lastTouchTime", lastTouchTime)
                .toString();
    }

    public enum State{
        IDLING, PREPARING, RUNNING
    }
}
