package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by Barak Bar Orion
 * 4/16/15.
 */
@Entity
public class JobRequest {
    @Id
    private String id;

    public JobRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
