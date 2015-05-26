package com.gigaspaces.newman.beans.criteria;

import com.gigaspaces.newman.utils.ToStringBuilder;

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
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("criteria", criteria)
                .toString();
    }
}
