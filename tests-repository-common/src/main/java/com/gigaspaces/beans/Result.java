package com.gigaspaces.beans;

/**
 * Created by Barak Bar Orion
 * 4/7/15.
 */
public class Result {

    private String status;

    public Result() {
    }

    public Result(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
