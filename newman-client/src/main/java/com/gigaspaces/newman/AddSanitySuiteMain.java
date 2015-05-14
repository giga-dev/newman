package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Suite;
import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.criteria.OrCriteria;
import com.gigaspaces.newman.beans.criteria.TestCriteria;
import com.gigaspaces.newman.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moran on 5/14/15.
 */
public class AddSanitySuiteMain {

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            throw new IllegalArgumentException("permutation file path argument missing");
        }
        File permutationFile = new File(args[0]);
        Suite sanitySuite = new Suite();
        sanitySuite.setName("Sanity");
        sanitySuite.setCriteria(
                new OrCriteria(
                        getTestCriteriasFromPermutaionFile(permutationFile)
                ));

        NewmanClient newmanClient = Main.createNewmanClient();
        try {
            Suite suite = newmanClient.addSuite(sanitySuite).toCompletableFuture().get();
            System.out.println("saved suite: " + suite);
        }finally {
            newmanClient.close();;
        }
    }

    private static Criteria[] getTestCriteriasFromPermutaionFile(File permutationFile) throws Exception{
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
        }
        finally {
            br.close();
        }
        return criterias.toArray(new Criteria[criterias.size()]);
    }
}
