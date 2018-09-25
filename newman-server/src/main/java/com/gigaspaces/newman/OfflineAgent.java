package com.gigaspaces.newman;
import java.util.Date;

public class OfflineAgent {

    private String name;
    private String host;
    private String hostAddress;
    private Date lastTouchTime;

    public OfflineAgent(String name, String host, String hostAddress, Date lastTouchTime) {
        this.name = name;
        this.host = host;
        this.hostAddress = hostAddress;
        this.lastTouchTime = lastTouchTime;
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

    public Date getLastTouchTime() {
        return lastTouchTime;
    }
    public void setLastTouchTime(Date lastTouchTime) {
        this.lastTouchTime = lastTouchTime;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }
}
