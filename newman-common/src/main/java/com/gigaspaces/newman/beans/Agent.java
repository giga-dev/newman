package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.LocalDateTime;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
public class Agent {
    @Id
    private String id;
    private String host;
    private String jobId;
    private LocalDateTime lastTouchTime;

    public Agent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public LocalDateTime getLastTouchTime() {
        return lastTouchTime;
    }

    public void setLastTouchTime(LocalDateTime lastTouchTime) {
        this.lastTouchTime = lastTouchTime;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", jobId='" + jobId + '\'' +
                ", lastTouchTime=" + lastTouchTime +
                '}';
    }
}
