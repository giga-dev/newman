package com.gigaspaces.newman.beans.specification;

import com.gigaspaces.newman.entities.Build;
import com.gigaspaces.newman.entities.Job;
import com.gigaspaces.newman.entities.Test;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public class TestSpecifications {

    public static Specification<Test> findRecentTestsByNameArgsAndShaAndBranch(
            String testName,
            List<String> testArguments,
            String sha, // pass null if not filtering by sha
            List<String> branches
    ) {
        return (testRoot, query, cb) -> {
            // Declare roots manually for each table
            //Root<Test> testRoot = testRoot; // same as query.from(Test.class);
            Root<Job> jobRoot = query.from(Job.class);
            Root<Build> buildRoot = query.from(Build.class);

            List<Predicate> predicates = new ArrayList<>();

            // manual join via predicates
            predicates.add(cb.equal(testRoot.get("jobId"), jobRoot.get("id")));
            predicates.add(cb.equal(jobRoot.get("build").get("id"), buildRoot.get("id")));

            predicates.add(cb.or(
                    cb.equal(testRoot.get("status"), Test.Status.FAIL),
                    cb.equal(testRoot.get("status"), Test.Status.SUCCESS)
            ));

            predicates.add(cb.equal(testRoot.get("name"), testName));

            if (testArguments != null && !testArguments.isEmpty()) {
                for (String argument : testArguments) {
                    // This is the Postgres expression using `any` function
                    predicates.add(
                            cb.equal(
                                    cb.literal(argument),
                                    cb.function("ANY", String.class, testRoot.get("arguments"))
                            )
                    );
                }
            }

            // filtering by branch
            if (branches != null && !branches.isEmpty()) {
                predicates.add(buildRoot.get("branch").in(branches));
            }

            if (sha != null) {
                predicates.add(cb.equal(testRoot.get("sha"), sha));
            }

            query.orderBy(cb.desc(testRoot.get("endTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}


