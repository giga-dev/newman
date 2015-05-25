package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Suite;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.criteria.CriteriaBuilder;
import com.gigaspaces.newman.beans.criteria.PatternCriteria;
import com.gigaspaces.newman.beans.criteria.TestCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.exclude;
import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.include;

/**
 * Suite submitter using environment variables
 */
public class NewmanSuiteSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanSuiteSubmitter.class);

    //connection env variables
    private static final String NEWMAN_HOST = "NEWMAN_HOST";
    private static final String NEWMAN_PORT = "NEWMAN_PORT";
    private static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    private static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";

    //suite env variables
    private static final String NEWMAN_SUITE_NAME = "NEWMAN_SUITE_NAME";
    private static final String NEWMAN_CRITERIA_TEST_TYPE = "NEWMAN_CRITERIA_TEST_TYPE";
    private static final String NEWMAN_CRITERIA_INCLUDE_LIST = "NEWMAN_CRITERIA_INCLUDE_LIST";
    private static final String NEWMAN_CRITERIA_EXCLUDE_LIST = "NEWMAN_CRITERIA_EXCLUDE_LIST";
    private static final String NEWMAN_CRITERIA_PERMUTATION_FILE = "NEWMAN_CRITERIA_PERMUTATION_FILE";

    public static void main(String[] args) throws Exception {

        NewmanClient newmanClient = null;
        try {
            newmanClient = getNewmanClient();

            Suite suite = new Suite();
            suite.setName(getEnvironment(NEWMAN_SUITE_NAME, true /*required*/));
            suite.setCriteria(getNewmanSuiteCriteria());

            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("Added suite: " + result);

        } finally {
            if (newmanClient != null) {
                newmanClient.close();
            }
        }
    }

    private static NewmanClient getNewmanClient() throws Exception {
        // connection arguments
        String host = getEnvironment(NEWMAN_HOST, true /*required*/);
        String port = getEnvironment(NEWMAN_PORT, true /*required*/);
        String username = getEnvironment(NEWMAN_USER_NAME, true /*required*/);
        String password = getEnvironment(NEWMAN_PASSWORD, true /*required*/);

        return NewmanClient.create(host, port, username, password);
    }

    private static Criteria[] parseCommaDelimitedList(String list) {
        if (list.length() == 0) return new Criteria[0];

        List<Criteria> listOfCriterias = new ArrayList<>();
        String[] split = list.trim().split(",");
        for (String criteria : split) {
            criteria = criteria.trim();
            if (notEmpty(criteria)) {
                listOfCriterias.add(PatternCriteria.containsCriteria(criteria));
            }
        }
        return listOfCriterias.toArray(new Criteria[listOfCriterias.size()]);
    }

    private static String getEnvironment(String var, boolean required) {
        String v = System.getenv(var);
        if (v == null && required) {
            logger.error("Please set the environment variable {} and try again.", var);
            throw new IllegalArgumentException("the environment variable " + var + " must be set");
        }
        return v;
    }

    private static Criteria[] getTestCriteriasFromPermutaionFile(String permutationFile) throws Exception {
        List<Criteria> criterias = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(permutationFile))) {
            try {
                while ((line = br.readLine()) != null) {
                    if (line.length() <= 1)
                        continue;
                    if (line.charAt(0) == '#')
                        continue;
                    TestCriteria criteria = TestCriteria.createCriteriaByTestArgs(line.split(" "));
                    System.out.println(criteria.getTest().getArguments());
                    criterias.add(criteria);
                }
            } finally {
                br.close();
            }
        }
        return criterias.toArray(new Criteria[criterias.size()]);
    }

    public static Criteria getNewmanSuiteCriteria() throws Exception {
        List<Criteria> criterias = new ArrayList<>();

        String testType = getEnvironment(NEWMAN_CRITERIA_TEST_TYPE, false /*required*/);
        if (notEmpty(testType)) {
            criterias.add(TestCriteria.createCriteriaByTestType(testType));
        }

        String includeList = getEnvironment(NEWMAN_CRITERIA_INCLUDE_LIST, false /*required*/);
        if (notEmpty(includeList)) {
            criterias.add(include(parseCommaDelimitedList(includeList)));
        }

        String excludeList = getEnvironment(NEWMAN_CRITERIA_EXCLUDE_LIST, false /*required*/);
        if (notEmpty(excludeList)) {
            criterias.add(exclude(parseCommaDelimitedList(excludeList)));
        }

        String permutationFile = getEnvironment(NEWMAN_CRITERIA_PERMUTATION_FILE, false /*required*/);
        if (notEmpty(permutationFile)) {
            Collections.addAll(criterias, getTestCriteriasFromPermutaionFile(permutationFile));
        }
        return CriteriaBuilder.join(criterias.toArray(new Criteria[criterias.size()]));
    }

    private static boolean notEmpty(String string) {
        return string != null && string.length() != 0;
    }
}
