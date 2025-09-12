package com.gigaspaces.newman.entities;

import javax.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.*;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
@Table(name = "agent", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Agent {

    @Id
    private String id;
    private String name;
    private String host;
    private String jobId;
    private Date lastTouchTime;
    private String hostAddress;
    private String pid;
    private int setupRetries = 0;
    private String groupName;
    private int workersCount;

    @Transient
    private Job job;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_current_tests", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "current_test")
    private Set<String> currentTests;

    @Enumerated(EnumType.STRING)
    private Agent.State state;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_capabilities", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "capability")
    private Set<String> capabilities;

    public Agent() {
        currentTests = new HashSet<>();
        capabilities = new TreeSet<>();
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

    public int getSetupRetries() {
        return setupRetries;
    }

    public void setSetupRetries(int setupRetries) {
        this.setupRetries = setupRetries;
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


    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Set<String> getCurrentTests() {
        return currentTests;
    }

    public void setCurrentTests(Set<String> currentTests) {
        this.currentTests = currentTests;
    }

    public void addCurrentTest(String testName) {
        this.currentTests.add(testName);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getWorkersCount() {
        return workersCount;
    }

    public void setWorkersCount(int workersCount) {
        this.workersCount = workersCount;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("host", host)
                .append("jobId", jobId)
                .append("lastTouchTime", lastTouchTime)
                .append("currentTests", currentTests)
                .append("state", state)
                .append("hostAddress", hostAddress)
                .append("pid", pid)
                .append("capabilities", capabilities)
                .append("job", job)
                .append("setupRetries", setupRetries)
                .append("groupName", groupName)
                .append("workersCount", workersCount)
                .toString();
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getPid() {
        return pid;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public enum State{
        IDLING, PREPARING, RUNNING
    }
}
