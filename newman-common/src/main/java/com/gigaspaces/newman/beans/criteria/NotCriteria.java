package com.gigaspaces.newman.beans.criteria;

import org.apache.commons.lang3.builder.ToStringBuilder;

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

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("criteria", criteria)
                .toString();
    }
}
