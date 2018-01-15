package com.gigaspaces.newman;

public class ServerStatus {
    public enum Status {
        RUNNING, SUSPENDED
    }

    private Status status;

    public ServerStatus(Status status) {
        this.status = status;
    }

    public ServerStatus() {
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
