package com.gigaspaces.newman.beans.criteria;

import java.util.Arrays;
import java.util.List;

/**
 * An OR composition of Criteria
 */
public class OrCriteria implements Criteria {

    private List<Criteria> criterias;

    public OrCriteria() {
    }

    public OrCriteria(Criteria... criteria) {
        criterias = Arrays.asList(criteria);
    }

    public List<Criteria> getCriterias() {
        return criterias;
    }

    public void setCriterias(List<Criteria> criterias) {
        this.criterias = criterias;
    }
}
