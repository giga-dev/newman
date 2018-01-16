package com.gigaspaces.newman;

public class ServerStatus {
    public enum Status {
        RUNNING, SUSPENDING, SUSPENDED, SUSPEND_FAILED
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
