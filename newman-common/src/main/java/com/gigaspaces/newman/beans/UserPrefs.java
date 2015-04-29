package com.gigaspaces.newman.beans;

import java.util.Set;

/**
 * Created by Barak Bar Orion
 * 4/29/15.
 */
public class UserPrefs {
    private String userName;
    private Set<String> roles;

    public UserPrefs() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
