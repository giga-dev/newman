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
            // Join with suite
            Join<Job, Suite> suiteJoin = root.join("suite");

            // Now, let's use cb.function("any", ...) for the "any" Postgres operator.
            // This checks if the `requirements` array contains any of the `capabilities`

            List<Expression<Boolean>> predicates = new ArrayList<>();

            for (String capability : capabilities) {
                // This is the Postgres expression using `any` function
                Expression<Boolean> condition = cb.equal(
                        cb.literal(capability),
                        cb.function("ANY", String.class, suiteJoin.get("requirements"))
                );

                predicates.add(condition);
            }

            // Combine all predicates with OR (you want to match if ANY of the capabilities is present)
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Job> hasNoRequirements() {
        return (root, query, cb) -> {
            Join<Job, Suite> suiteJoin = root.join("suite");
            return cb.or(
                    cb.isNull(suiteJoin.get("requirements")), // array is null
                    cb.equal(getArraySize(cb, suiteJoin.get("requirements")), 0) // array is empty (length is 0)
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
    private static Specification<Job> preparingAgentsLessThanRequired() {
        return (root, query, cb) -> {
            Expression<Integer> totalPlannedTests = cb.sum(root.get("totalTests"), root.get("numOfTestRetries"));
            Expression<Integer> alreadyProcessedTests = cb.sum(
                    root.get("passedTests"),
                    cb.sum(root.get("failedTests"), root.get("runningTests"))
            );
            Expression<Integer> requiredAgents = cb.diff(totalPlannedTests, alreadyProcessedTests);

            // Predicate: preparingAgents.size < requiredAgents
            return cb.lessThan(getArraySize(cb, root.get("preparingAgents")), requiredAgents);
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
    public static Specification<Job> preparingAgentsCondition() {
        return Specification.where(preparingAgentsLessThanRequired()).or(preparingAgentsDoesNotExist());
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

