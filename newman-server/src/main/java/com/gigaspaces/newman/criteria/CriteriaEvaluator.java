package com.gigaspaces.newman.criteria;

import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.criteria.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by moran
 * on 5/4/15.
 */
public class CriteriaEvaluator {

    private final CompiledCriteria compiledCriteria;

    public CriteriaEvaluator(Criteria criteria) {
        compiledCriteria = compile(criteria);
    }

    private CompiledCriteria compile(Criteria criteria) {
        if (criteria instanceof AndCriteria) {
            return compileAndCriteria((AndCriteria) criteria);
        } else if (criteria instanceof OrCriteria) {
            return compileOrCriteria(((OrCriteria) criteria));
        } else if (criteria instanceof NotCriteria) {
            return compileNotCriteria(((NotCriteria) criteria));
        } else if (criteria instanceof PatternCriteria) {
            return compilePatternCriteria(((PatternCriteria) criteria));
        } else if (criteria instanceof PropertyCriteria) {
            return compilePropertyCriteria(((PropertyCriteria) criteria));
        } else if (criteria instanceof SpecificTestsCriteria) {
            return compileSpecificTestsCriteria(((SpecificTestsCriteria) criteria));
        } else if (criteria instanceof TypeCriteria) {
            return compileTestCriteria(((TypeCriteria) criteria));
        }
        return null;
    }

    private CompiledCriteria compileTestCriteria(TypeCriteria criteria) {
        return new TypeCompiledCriteria(criteria.getType());
    }

    private CompiledCriteria compileSpecificTestsCriteria(SpecificTestsCriteria criteria) {
        return new SpecificTestsCompiledCriteria(criteria.getTestNames());
    }

    private CompiledCriteria compilePropertyCriteria(PropertyCriteria criteria) {
        return new PropertyCompiledCriteria(criteria.getKey(), criteria.getValue());
    }

    private CompiledCriteria compilePatternCriteria(PatternCriteria criteria) {
        return new PatternCompiledCriteria(criteria.getRegex());
    }

    private CompiledCriteria compileNotCriteria(NotCriteria criteria) {
        return new NotCompiledCriteria(compile(criteria.getCriteria()));
    }

    private CompiledCriteria compileOrCriteria(OrCriteria criteria) {
        return new OrCompiledCriteria(compileAll(criteria.getCriterias()));
    }

    private CompiledCriteria compileAndCriteria(AndCriteria criteria) {
        return new AndCompiledCriteria(compileAll(criteria.getCriterias()));
    }

    private List<CompiledCriteria> compileAll(List<Criteria> criteriaList) {
        return criteriaList.stream().map(this::compile).collect(Collectors.toList());
    }


    public boolean evaluate(Test test) {
        return compiledCriteria.accept(test);
    }
}
