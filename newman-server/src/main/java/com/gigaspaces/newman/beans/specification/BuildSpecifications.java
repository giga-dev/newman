package com.gigaspaces.newman.beans.specification;

import com.gigaspaces.newman.entities.Build;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BuildSpecifications {

    public static Specification<Build> hasAllTags(Set<String> tags) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String tag : tags) {
                predicates.add(cb.isMember(tag, root.get("tags")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));    // check for ALL tags FULL match
        };
    }

    public static Specification<Build> hasBranch(String branch) {
        return (root, query, cb) -> cb.equal(root.get("branch"), branch);
    }

    public static Specification<Build> hasAllJobsCompleted() {
        return (root, query, cb) -> {
            Expression<Integer> totalJobs = root.get("buildStatus").get("totalJobs");
            Expression<Integer> doneJobs = root.get("buildStatus").get("doneJobs");
            Expression<Integer> brokenJobs = root.get("buildStatus").get("brokenJobs");

            Predicate hasJobs = cb.greaterThan(totalJobs, 0);
            Predicate allCompleted = cb.equal(cb.sum(doneJobs, brokenJobs), totalJobs);

            return cb.and(hasJobs, allCompleted);
        };
    }
    /* ==================================================================================== */
    public static Specification<Build> matchesBranch(String branchStr) {
        return (root, query, cb) -> cb.equal(root.get("branch"), branchStr);
    }

    public static Specification<Build> hasNoTags(Set<String> excludeTags) {
        return (root, query, cb) -> {
            if (excludeTags == null || excludeTags.isEmpty()) return cb.conjunction();

            // Subquery style exclusion: no tag in the set should exist in b.tags
            return cb.not(root.join("tags").in(excludeTags));
        };
    }

    public static Specification<Build> hasTags(Set<String> tagsSet) {
        return (root, query, cb) -> {
            if (tagsSet == null || tagsSet.isEmpty()) return cb.conjunction();

            // Build has all of the tags in the set
            Predicate[] predicates = tagsSet.stream()
                    .map(tag -> cb.isMember(tag, root.get("tags")))
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }

    public static Specification<Build> hasNoJobsInBuildStatus() {
        return (root, query, cb) -> cb.equal(root.get("buildStatus").get("totalJobs"), 0);
    }
}

