package com.gigaspaces.newman.criteria;


import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.criteria.*;
import com.gigaspaces.newman.beans.criteria.CriteriaEvaluator;
import org.junit.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by
 * moran on 5/4/15.
 */
public class CriteriaEvaluatorTest {

    @org.junit.Test
    public void test_PropertyCriteria() {

        PropertyCriteria propertyCriteria = new PropertyCriteria("os", "linux");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(propertyCriteria);

        Map<String, String> props = new HashMap<>();
        Test test = new Test();
        test.setProperties(props);

        props.put("os", "linux");
        assertTrue(criteriaEvaluator.evaluate(test));

        props.put("os", "windows");
        Assert.assertFalse(criteriaEvaluator.evaluate(test));
    }

    @org.junit.Test
    public void test_PatternCriteria_RecursivePackageNameCriteria() {

        PatternCriteria patternCriteria = PatternCriteria.recursivePackageNameCriteria("com.gigaspaces.test");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(patternCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.foo.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("com.foo.FooTest");
        assertFalse(criteriaEvaluator.evaluate(test3));

        Test test4 = new Test();
        test4.setName("com_gigaspaces_test_MyTest");
        assertFalse(criteriaEvaluator.evaluate(test4));
    }

    @org.junit.Test
    public void test_PatternCriteria_NonRecursivePackageNameCriteria() {

        PatternCriteria patternCriteria = PatternCriteria.nonRecursivePackageNameCriteria("com.gigaspaces.test");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(patternCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.foo.MyTest");
        assertFalse(criteriaEvaluator.evaluate(test2));
    }

    @org.junit.Test
    public void test_PatternCriteria_ClassNameCriteria() {

        PatternCriteria patternCriteria = PatternCriteria.classNameCriteria("com.gigaspaces.test.MyTest");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(patternCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.foo.MyTest");
        assertFalse(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("com.gigaspaces.test.MyTestFoo");
        assertFalse(criteriaEvaluator.evaluate(test3));

        Test test4 = new Test();
        test4.setName("com.gigaspaces.test.MyTest.Foo");
        assertFalse(criteriaEvaluator.evaluate(test4));
    }

    @org.junit.Test
    public void test_PatternCriteria_startsWithCriteria() {

        PatternCriteria patternCriteria = PatternCriteria.startsWithCriteria("com.gigaspaces.test.My");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(patternCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.My");
        assertTrue(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("com.gigaspaces.test.M");
        assertFalse(criteriaEvaluator.evaluate(test3));
    }

    @org.junit.Test
    public void test_PatternCriteria_endsWithCriteria() {

        PatternCriteria patternCriteria = PatternCriteria.endsWithCriteria(".MyTest");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(patternCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.testMyTest");
        assertFalse(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("com.gigaspaces.MyTesttt");
        assertFalse(criteriaEvaluator.evaluate(test3));
    }

    @org.junit.Test
    public void test_PatternCriteria_containsCriteria() {
        PatternCriteria patternCriteria = PatternCriteria.containsCriteria(".gigaspaces.");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(patternCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("comgigaspaces.MyTest");
        assertFalse(criteriaEvaluator.evaluate(test3));

        Test test4 = new Test();
        test4.setName("gigaspaces.MyTest");
        assertFalse(criteriaEvaluator.evaluate(test4));

        Test test5 = new Test();
        test5.setName("comgigaspacesMyTest");
        assertFalse(criteriaEvaluator.evaluate(test5));
    }

        @org.junit.Test
    public void test_NotCriteria() {
        NotCriteria notCriteria = new NotCriteria( PatternCriteria.classNameCriteria("com.gigaspaces.test.MyTest"));
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(notCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.MyTest");
        assertFalse(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.text.YourTest");
        assertTrue(criteriaEvaluator.evaluate(test2));
    }

    @org.junit.Test
    public void test_AndCriteria() {
        AndCriteria andCriteria = new AndCriteria(
                new PropertyCriteria("key1", "val1"),
                new PropertyCriteria("key2", "val2"));
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(andCriteria);

        Map<String,String> props = new HashMap<>();
        Test test = new Test();
        test.setProperties(props);

        props.put("key1", "val1");
        assertFalse(criteriaEvaluator.evaluate(test));

        props.put("key2", "val2");
        assertTrue(criteriaEvaluator.evaluate(test));

        props.put("key3", "val3");
        assertTrue(criteriaEvaluator.evaluate(test));
    }

    @org.junit.Test
    public void test_OrCriteria() {
        OrCriteria orCriteria = new OrCriteria(
                new PropertyCriteria("key1", "val1"),
                new PropertyCriteria("key2", "val2"));
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(orCriteria);

        Map<String,String> props = new HashMap<>();
        Test test = new Test();

        //no properties
        assertFalse(criteriaEvaluator.evaluate(test));

        //empty properties
        test.setProperties(props);
        assertFalse(criteriaEvaluator.evaluate(test));

        //non-match property
        props.put("key3", "val3");
        assertFalse(criteriaEvaluator.evaluate(test));

        //one-matching property
        props.put("key1", "val1");
        assertTrue(criteriaEvaluator.evaluate(test));

        //two-matching properties
        props.put("key2", "val2");
        assertTrue(criteriaEvaluator.evaluate(test));
    }

    @org.junit.Test
    public void test_TestCriteria_byTestName() {
        TestCriteria testCriteria = TestCriteria.createCriteriaByTestName("com.gigaspaces.test.MyTest");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(testCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.Test");
        assertFalse(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test2));
    }

    @org.junit.Test
    public void test_TestCriteria_byType() {
        TestCriteria testCriteria = TestCriteria.createCriteriaByTestType("myType");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(testCriteria);

        Test test1 = new Test();
        test1.setTestType("myType");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setTestType("otherType");
        assertFalse(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        assertFalse(criteriaEvaluator.evaluate(test3));
    }

    @org.junit.Test
    public void test_TestCriteria_byArgs() {
        TestCriteria testCriteria = TestCriteria.createCriteriaByTestArgs("remote");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(testCriteria);

        Test test1 = new Test();
        test1.setArguments(Arrays.asList("remote"));
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setArguments(Arrays.asList("LRU", "remote"));
        assertTrue(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setArguments(Arrays.asList("embedded"));
        assertFalse(criteriaEvaluator.evaluate(test3));

        Test test4 = new Test();
        assertFalse(criteriaEvaluator.evaluate(test4));
    }

    @org.junit.Test
    public void test_CriteriaBuilder() {

        Criteria criteria = CriteriaBuilder.join(
                CriteriaBuilder.include(
                        PatternCriteria.containsCriteria(".security"),
                        PatternCriteria.containsCriteria(".gateway.")),
                CriteriaBuilder.exclude(
                        PatternCriteria.containsCriteria(".disconnect."),
                        PatternCriteria.containsCriteria(".DBTest#"))
        );

        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(criteria);

        Test test1 = new Test();
        test1.setName("com.test.security.Test");
        assertTrue(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.test.gateway.Test");
        assertTrue(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("com.test.disconnect.Test");
        assertFalse(criteriaEvaluator.evaluate(test3));

        Test test4 = new Test();
        test4.setName("com.test.disconnect.DBTest#foo");
        assertFalse(criteriaEvaluator.evaluate(test4));

        Test test5 = new Test();
        test5.setName("com.test.MyTest");
        assertFalse(criteriaEvaluator.evaluate(test5));
    }
}
