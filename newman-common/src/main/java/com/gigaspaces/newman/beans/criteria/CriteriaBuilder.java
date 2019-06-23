package com.gigaspaces.newman.beans.criteria;

import java.util.Arrays;
import java.util.List;

/**
 * Created by moran on 5/20/15.
 */
public class CriteriaBuilder {

    public static Criteria join(Criteria... criterias) {
        AndCriteria andCriteria = new AndCriteria();
        andCriteria.setCriterias(Arrays.asList(criterias));
        return andCriteria;
    }

    public static Criteria include(Criteria... criterias) {
        OrCriteria orCriteria = new OrCriteria();
        orCriteria.setCriterias(Arrays.asList(criterias));
        return orCriteria;
    }

    public static Criteria include(List<Criteria> criterias) {
        OrCriteria orCriteria = new OrCriteria();
        orCriteria.setCriterias(criterias);
        return orCriteria;
    }

    public static Criteria exclude(Criteria... criterias) {
        OrCriteria orCriteria = new OrCriteria();
        orCriteria.setCriterias(Arrays.asList(criterias));

        NotCriteria notCriteria = new NotCriteria();
        notCriteria.setCriteria(orCriteria);
        return notCriteria;
    }
}
