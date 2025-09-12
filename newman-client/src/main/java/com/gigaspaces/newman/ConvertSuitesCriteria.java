package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Batch;
import com.gigaspaces.newman.entities.Suite;
import com.gigaspaces.newman.beans.criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;



public class ConvertSuitesCriteria {


    private NewmanClient newmanClient;

    public ConvertSuitesCriteria(NewmanClient newmanClient) {
        this.newmanClient = newmanClient;
    }

    public void convert() throws ExecutionException, InterruptedException {
        Batch<Suite> suites = newmanClient.getAllSuites().toCompletableFuture().get();
        for (Suite suite : suites.getValues()) {
            String test_type = "";
            String[] str = suite.getCustomVariables().split(",");
            if (str[0].contains("SUITE_TYPE")) {
                String[] type = str[0].split("=");
                test_type = type[1];
            }
            Criteria criterias = suite.getCriteria();
            if (criterias instanceof AndCriteria) {
                List<Criteria> list = ((AndCriteria) criterias).getCriterias();
                SuiteCriteria suiteCriteria = createSuiteCriteria(list, test_type);
                Suite s = new Suite();
                s.setCriteria(suiteCriteria);
                s.setName(suite.getName());
                s.setCustomVariables(suite.getCustomVariables());
                Suite suite1 = newmanClient.addSuite(s).toCompletableFuture().get();
            }
        }
    }
    private static SuiteCriteria createSuiteCriteria(List<Criteria> criteria_list, String suiteType)
    {
        ArrayList include =new ArrayList<>();
        ArrayList exclude = new ArrayList();
        for(Criteria criteria : criteria_list) {
            if (criteria instanceof OrCriteria) {
                List<Criteria> orCriterias_list = ((OrCriteria) criteria).getCriterias();
                for (Criteria criteria2 : orCriterias_list) {
                    include.add(criteria2);
                }
            } else if (criteria instanceof PatternCriteria || criteria instanceof TestCriteria) {
                include.add(criteria);
            } else if (criteria instanceof NotCriteria) {
                Criteria c = ((NotCriteria) criteria).getCriteria();
                if (c instanceof OrCriteria) {
                    for (Criteria criteria2 : ((OrCriteria) c).getCriterias()) {
                        exclude.add(criteria2);
                    }
                } else {
                    exclude.add(c);
                }

            }
        }

        return new SuiteCriteria(include,exclude,suiteType);

    }


}
