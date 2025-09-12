package com.gigaspaces.newman.beans.criteria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gigaspaces.newman.entities.Test;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * A test criteria for matching (test name, arguments, type). Only non-null field are matched.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCriteria implements Criteria {

    private Test test;

    public TestCriteria() {
    }

    public TestCriteria(Test test) {
        this.test = test;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public static TestCriteria createCriteriaByTestName(String testName) {
        Test test = new Test();
        test.setName(testName);
        return new TestCriteria(test);
    }

    public static TestCriteria createCriteriaByTestType(String testType) {
        Test test = new Test();
        test.setTestType(testType);
        return new TestCriteria(test);
    }

    public static TestCriteria createCriteriaByTestArgs(String... testArgs) {
        Test test = new Test();
        test.setArguments(Arrays.asList(testArgs));
        return new TestCriteria(test);
    }

    public static TestCriteria createCriteriaByTestArgs(List<String> testArgs) {
        Test test = new Test();
        test.setArguments(testArgs);
        return new TestCriteria(test);
    }

    public static TestCriteria createUnifiedCriteria(TestCriteria... testCriterias) {
        Test test = new Test();
        for (TestCriteria testCriteria : testCriterias) {
            Test criteria = testCriteria.getTest();
            if (criteria.getName() != null) {
                test.setName(criteria.getName());
            }
            if (criteria.getTestType() != null) {
                test.setTestType(criteria.getTestType());
            }
            if (criteria.getArguments() != null) {
                test.setArguments(criteria.getArguments());
            }
        }
        return new TestCriteria(test);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("test", test)
                .toString();
    }
}
