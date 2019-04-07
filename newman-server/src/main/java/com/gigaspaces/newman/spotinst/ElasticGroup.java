package com.gigaspaces.newman.spotinst;


public class ElasticGroup {
    private String name;
    private String id;
    private ElasticGroupDescription description;
    private ElasticGroupCapacity capacity;
    private int connectedAgents;
    private int runningVMs;

    public ElasticGroup() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ElasticGroupDescription getDescription() {
        return description;
    }

    public void setDescription(ElasticGroupDescription description) {
        this.description = description;
    }

    public ElasticGroupCapacity getCapacity() {
        return capacity;
    }

    public void setCapacity(ElasticGroupCapacity capacity) {
        this.capacity = capacity;
    }

    public int getConnectedAgents() {
        return connectedAgents;
    }

    public void setConnectedAgents(int connectedAgents) {
        this.connectedAgents = connectedAgents;
    }

    public int getRunningVMs() {
        return runningVMs;
    }

    public void setRunningVMs(int runningVMs) {
        this.runningVMs = runningVMs;
    }

    @Override
    public String toString() {
        return "ElasticGroup{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", capacity=" + capacity +
                ", connectedAgents=" + connectedAgents +
                ", runningVMs=" + runningVMs +
                '}';
    }
}
