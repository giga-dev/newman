package com.gigaspaces.newman.spotinst;


import com.google.gson.annotations.SerializedName;

public class ElasticGroup {
    private String name;
    private String id;

    private ElasticGroupCapacity capacity;
    @SerializedName("compute")
    private ElasticGroupTags tags;
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

    public ElasticGroupTags getTags() {
        return tags;
    }

    public void setTags(ElasticGroupTags tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "ElasticGroup{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", capacity=" + capacity +
                ", tags=" + tags +
                ", connectedAgents=" + connectedAgents +
                ", runningVMs=" + runningVMs +
                '}';
    }
}
