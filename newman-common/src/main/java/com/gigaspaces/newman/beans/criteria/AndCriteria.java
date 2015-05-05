package com.gigaspaces.newman.beans.criteria;

import java.util.Arrays;
import java.util.List;

/**
 * an AND Composition of Criteria
 */
public class AndCriteria implements Criteria {

    private List<Criteria> criterias;

    public AndCriteria() {
    }

    public AndCriteria(Criteria... criteria) {
        criterias = Arrays.asList(criteria);
    }

    public List<Criteria> getCriterias() {
        return criterias;
    }

    public void setCriterias(List<Criteria> criterias) {
        this.criterias = criterias;
    }
}
