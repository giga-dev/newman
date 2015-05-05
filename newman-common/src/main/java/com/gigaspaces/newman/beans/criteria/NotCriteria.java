package com.gigaspaces.newman.beans.criteria;

/**
 * A negation of a criteria
 */
public class NotCriteria implements Criteria {

    private Criteria criteria;

    public NotCriteria() {
    }

    public NotCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }
}
