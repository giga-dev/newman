package com.gigaspaces.newman.criteria;


import com.gigaspaces.newman.beans.Test;
import com.gigaspaces.newman.beans.criteria.*;
import org.junit.Assert;

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
    public void test_SpecificTestsCriteria() {
        SpecificTestsCriteria specificTestsCriteria = new SpecificTestsCriteria(
                "com.gigaspaces.test.MyTest",
                "com.gigaspaces.test.foo.FooTest");
        CriteriaEvaluator criteriaEvaluator = new CriteriaEvaluator(specificTestsCriteria);

        Test test1 = new Test();
        test1.setName("com.gigaspaces.test.Test");
        assertFalse(criteriaEvaluator.evaluate(test1));

        Test test2 = new Test();
        test2.setName("com.gigaspaces.test.MyTest");
        assertTrue(criteriaEvaluator.evaluate(test2));

        Test test3 = new Test();
        test3.setName("com.gigaspaces.test.foo.FooTest");
        assertTrue(criteriaEvaluator.evaluate(test3));
    }

}
