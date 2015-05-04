package com.gigaspaces.newman.beans.criteria;

/**
 * Created by Barak Bar Orion
 * 5/4/15.
 */
public class Pattern implements Criteria {
    private String match;

    public Pattern() {
    }

    public Pattern(String match) {
        this.match = match;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }
}
