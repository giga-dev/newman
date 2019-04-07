package com.gigaspaces.newman.spotinst;

public class ElasticGroupDescription {
    private String groupName;
    private String description;
    private String owner;

    public ElasticGroupDescription() {
    }

    public ElasticGroupDescription(String groupName, String description, String owner) {
        this.groupName = groupName;
        this.description = description;
        this.owner = owner;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ElasticGroupDescription{" +
                "groupName='" + groupName + '\'' +
                ", description='" + description + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
