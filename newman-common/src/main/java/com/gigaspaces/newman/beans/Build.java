package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Build {
    @Id
    private String id;
    private Map<String, String> shas;
    private String branch;
    private URI uri;
    private State state;

    public Build() {
        state = State.READY;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getShas() {
        return shas;
    }

    public void setShas(Map<String, String> shas) {
        this.shas = shas;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Build{" +
                "id='" + id + '\'' +
                ", shas=" + shas +
                ", branch='" + branch + '\'' +
                ", uri=" + uri +
                ", state=" + state +
                '}';
    }
}
