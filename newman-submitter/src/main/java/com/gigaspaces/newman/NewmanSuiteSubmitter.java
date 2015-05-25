package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.beans.Suite;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.criteria.CriteriaBuilder;
import com.gigaspaces.newman.beans.criteria.TestCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.exclude;
import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.join;
import static com.gigaspaces.newman.beans.criteria.PatternCriteria.classNameCriteria;
import static com.gigaspaces.newman.beans.criteria.PatternCriteria.nonRecursivePackageNameCriteria;
import static com.gigaspaces.newman.beans.criteria.PatternCriteria.recursivePackageNameCriteria;
import static com.gigaspaces.newman.beans.criteria.TestCriteria.createCriteriaByTestType;

/**
 * Created by moran on 5/25/15.
 */
public class NewmanSuiteSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanSuiteSubmitter.class);

    private static final String NEWMAN_HOST = "NEWMAN_HOST";
    private static final String NEWMAN_PORT = "NEWMAN_PORT";
    private static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    private static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            String name = NewmanSuiteSubmitter.class.getSimpleName();
            throw new IllegalArgumentException("Usage: " + name + " [-n | -nightly] [-s | -sanity suite_name permutations_file_path]");
        }

        NewmanClient newmanClient = null;
        try {
            newmanClient = getNewmanClient();

            String synopsis = args[0];
            switch (synopsis) {
                case "-n":
                case "-nightly":
                    createNightlySuite(newmanClient);
                    break;
                case "-s":
                case "-sanity":
                    if (args.length != 3) {
                        throw new IllegalArgumentException("wrong usage: [-s | -sanity suite_name permutations_file_path]");
                    }
                    String suiteName = args[1];
                    String permutationFilePath = args[2];
                    createSanitySuite(newmanClient, suiteName, permutationFilePath);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected argument");
            }
        } finally {
            if (newmanClient != null) {
                newmanClient.close();
            }
        }
    }

    private static NewmanClient getNewmanClient() throws Exception {
        // connection arguments
        String host = getEnvironment(NEWMAN_HOST);
        String port = getEnvironment(NEWMAN_PORT);
        String username = getEnvironment(NEWMAN_USER_NAME);
        String password = getEnvironment(NEWMAN_PASSWORD);

        NewmanClient newmanClient = NewmanClient.create(host, port, username, password);
        return newmanClient;
    }

    private static String getEnvironment(String var) {
        String v = System.getenv(var);
        if (v == null) {
            logger.error("Please set the environment variable {} and try again.", var);
            throw new IllegalArgumentException("the environment variable " + var + " must be set");
        }
        return v;
    }

    private static void createNightlySuite(NewmanClient newmanClient) throws Exception {
        Suite suite = new Suite();
        suite.setName("Nightly Regression");
        suite.setCriteria(
                join(
                        createCriteriaByTestType("tgrid"),
                        exclude(
                                classNameCriteria("com.gigaspaces.test.database.sql.PerformanceTest"),
                                nonRecursivePackageNameCriteria("com.gigaspaces.test.tg"),
                                nonRecursivePackageNameCriteria("com.gigaspaces.test.stress.map"),
                                recursivePackageNameCriteria("com.gigaspaces.test.blobstore")
                        )
                ));

        Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
        logger.info("Added suite: " + result);
    }


    private static void createSanitySuite(NewmanClient newmanClient, String suiteName, String permutationFilePath) throws Exception {
        File file = new File(permutationFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist");
        }
        Suite suite = new Suite();
        suite.setName(suiteName);
        suite.setCriteria(
                join(
                        getTestCriteriasFromPermutaionFile(file)
                ));


        Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
        logger.info("Added suite: " + result);
    }

    private static Criteria[] getTestCriteriasFromPermutaionFile(File permutationFile) throws Exception {
        List<Criteria> criterias = new ArrayList<>();
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(permutationFile));
        try {
            StringBuilder sb = new StringBuilder();

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
        return criterias.toArray(new Criteria[criterias.size()]);
    }

}
