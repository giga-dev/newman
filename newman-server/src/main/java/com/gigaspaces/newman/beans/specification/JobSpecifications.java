package com.gigaspaces.newman.beans.specification;

import com.gigaspaces.newman.beans.State;
import com.gigaspaces.newman.entities.Job;
import com.gigaspaces.newman.entities.Suite;

import javax.persistence.criteria.*;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JobSpecifications {

    public static Specification<Job> isReadyOrRunning() {
        return (root, query, cb) -> cb.and(
                cb.or(
                        cb.equal(root.get("state"), State.READY),
                        cb.equal(root.get("state"), State.RUNNING)
                ),
                cb.notEqual(
                        cb.sum(root.get("totalTests"), root.get("numOfTestRetries")),
                        cb.sum(cb.sum(root.get("passedTests"), root.get("failedTests")), root.get("runningTests"))
                )
        );
    }

    public static Specification<Job> whereJobId(String jobId) {
        return (root, query, cb) -> cb.equal(root.get("id"), jobId);
    }

    public static Specification<Job> hasPreparingAgents() {
        return (root, query, cb) -> {
            // array_length(preparing_agents, 1)
            Expression<Integer> preparingAgentsLength = getArraySize(cb, root.get("preparingAgents"));

            // totalTests + numOfTestRetries - passedTests - failedTests - runningTests
            Expression<Integer> neededAgentsCount = cb.sum(
                    cb.sum(root.get("totalTests"), root.get("numOfTestRetries")), // this.totalTests + this.numOfTestRetries
                    cb.neg(cb.sum(                                                   // minus
                            cb.sum(root.get("passedTests"), root.get("failedTests")),   // this.passedTests + this.failedTests + this.runningTests
                            root.get("runningTests")
                    ))
            );

            // CASE 1: preparingAgents exists AND its preparingAgents.length < neededAgentsCount
            Predicate preparingAgentsExistsAndLessThanNeeded = cb.lessThan(preparingAgentsLength, neededAgentsCount);

            // CASE 2: preparingAgents does not exist => represented by empty array or null in SQL
            Predicate preparingAgentsIsEmpty = cb.equal(getArraySize(cb, root.get("preparingAgents")), 0);

            // totalTests != 0
            Predicate totalTestsNonZero = cb.notEqual(root.get("totalTests"), 0);

            return cb.and(
                    cb.or(preparingAgentsIsEmpty, preparingAgentsExistsAndLessThanNeeded), // less than needed or no agents at all
                    totalTestsNonZero   // and has tests to run
            );
        };
    }

    public static Specification<Job> hasAnyOfCapabilitiesInRequirements(Set<String> capabilities) {
        return (root, query, cb) -> {
            // Create manual join: FROM job, suite WHERE job.suite_id = suite.id
            Root<Suite> suiteRoot = query.from(Suite.class);

            List<Predicate> predicates = new ArrayList<>();

            // Join condition: suite.id = job.suiteId
            predicates.add(cb.equal(suiteRoot.get("id"), root.get("suiteId")));

            // Capability conditions
            List<Predicate> capabilityPredicates = new ArrayList<>();
            for (String capability : capabilities) {
                capabilityPredicates.add(
                    cb.equal(
                        cb.literal(capability),
                        cb.function("ANY", String.class, suiteRoot.get("requirements"))
                    )
                );
            }
            predicates.add(cb.or(capabilityPredicates.toArray(new Predicate[0])));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Job> hasNoRequirements() {
        return (root, query, cb) -> {
            // Use subquery to check if Suite exists and has no requirements
            // OR if Suite doesn't exist at all (deleted)

            Subquery<String> subquery = query.subquery(String.class);
            Root<Suite> suiteSubRoot = subquery.from(Suite.class);

            // Find suites with no requirements
            subquery.select(suiteSubRoot.get("id"))
                    .where(cb.or(
                        cb.isNull(suiteSubRoot.get("requirements")),
                        cb.equal(getArraySize(cb, suiteSubRoot.get("requirements")), 0)
                    ));

            // Job matches if: suiteId is in subquery (suite exists with no requirements)
            // OR suiteId is NOT in (select id from suite) - meaning suite was deleted
            Subquery<String> allSuitesSubquery = query.subquery(String.class);
            Root<Suite> allSuitesRoot = allSuitesSubquery.from(Suite.class);
            allSuitesSubquery.select(allSuitesRoot.get("id"));

            return cb.or(
                root.get("suiteId").in(subquery),  // Suite exists and has no requirements
                cb.not(root.get("suiteId").in(allSuitesSubquery))  // Suite doesn't exist (deleted)
            );
        };
    }

    public static Specification<Job> hasAgentGroup(String agentGroup) {
        return (root, query, cb) -> cb.equal(
                cb.literal(agentGroup),
                cb.function("ANY", String.class, root.get("agentGroups"))
        );
    }

    // Specification to check if preparingAgents exists and its size is less than the required number
    private static Specification<Job> preparingAgentsLessThanRequired(int agentWorkersCount) {
        return (root, query, cb) -> {
            Expression<Integer> totalPlannedTests = cb.sum(root.get("totalTests"), root.get("numOfTestRetries"));
            Expression<Integer> alreadyProcessedTests = cb.sum(
                    root.get("passedTests"),
                    cb.sum(root.get("failedTests"), root.get("runningTests"))
            );
            Expression<Integer> remainingTests = cb.diff(totalPlannedTests, alreadyProcessedTests);

            // Create manual join: FROM job, suite WHERE job.suite_id = suite.id
            // This will only match jobs where Suite still exists
            Root<Suite> suiteRoot = query.from(Suite.class);

            // Join condition: suite.id = job.suiteId
            Predicate joinCondition = cb.equal(suiteRoot.get("id"), root.get("suiteId"));

            // Get workersAllowed from Suite
            Expression<Integer> suiteWorkersAllowed = suiteRoot.get("workersAllowed");

            // determine effective workers per agent: min(agentWorkersCount, suiteWorkersAllowed)
            Expression<Integer> effectiveWorkers = cb.<Integer>selectCase()
                    .when(cb.lessThan(cb.literal(agentWorkersCount), suiteWorkersAllowed), cb.literal(agentWorkersCount))
                    .otherwise(suiteWorkersAllowed);

            // ceil division: (remaining + effectiveWorkers - 1) / effectiveWorkers
            Expression<Integer> numerator = cb.sum(remainingTests, cb.diff(effectiveWorkers, cb.literal(1)));
            Expression<Integer> requiredAgents = cb.quot(numerator, effectiveWorkers).as(Integer.class);

            // get current preparing agents count
            Expression<Integer> preparingAgentsCount = getArraySize(cb, root.get("preparingAgents"));

            // final predicate: join condition AND preparingAgents.size < requiredAgents
            return cb.and(
                    joinCondition,
                    cb.lessThan(preparingAgentsCount, requiredAgents)
            );
        };
    }

    // Specification to check if preparingAgents does not exist (is null or empty)
    private static Specification<Job> preparingAgentsDoesNotExist() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("preparingAgents")), // array is null
                cb.equal(getArraySize(cb, root.get("preparingAgents")), 0) // array is empty (length is 0)
        );
    }

    // Combined Specification for preparingAgents (either exists and size is less or doesn't exist)
    public static Specification<Job> preparingAgentsCondition(int agentWorkersCount) {
        return Specification.where(preparingAgentsLessThanRequired(agentWorkersCount)).or(preparingAgentsDoesNotExist());
    }

    private static Expression<Integer> getArraySize(CriteriaBuilder cb, Path<?> arrayField) {
        Expression<Integer> arrayLength = cb.function(
                "array_length",     // PostgreSQL function to get the array length
                Integer.class,          // The return type
                arrayField,  // The array field
                cb.literal(1)   // The dimension of the array (1 for single-dimensional arrays)
        );
        return cb.coalesce(arrayLength, cb.literal(0)); // array_length requires coalesce for empty arrays
    }

}
