package com.gigaspaces.newman.beans.criteria;

import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.criteria.compiled.*;

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
        } else if (criteria instanceof TestCriteria) {
            return compileTestCriteria(((TestCriteria) criteria));
        } else if (criteria instanceof ArgumentsCriteria) {
            return compileArgumentCriteria(((ArgumentsCriteria) criteria));
        } else if (criteria instanceof SuiteCriteria) {
            SuiteCriteria suiteCriteria = (SuiteCriteria) criteria;
            return compileSuiteCriteria(suiteCriteria.getInclude(), suiteCriteria.getExclude(), suiteCriteria.getSuiteType());
        }
        return null;
    }


    private CompiledCriteria compileArgumentCriteria(ArgumentsCriteria criteria) {
        return new ArgumentCompiledCriteria(criteria.getArg());
    }
    private CompiledCriteria compileSuiteCriteria(List<Criteria> include, List<Criteria> exclude, String type) {
        return new SuiteCompiledCriteria(compileAll(include), compileAll(exclude), type);
    }

    private CompiledCriteria compileTestCriteria(TestCriteria criteria) {
        return new TestCompiledCriteria(criteria.getTest());
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
