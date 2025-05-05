package com.gigaspaces.newman.beans.specification;

import com.gigaspaces.newman.entities.Agent;
import org.springframework.data.jpa.domain.Specification;

public class AgentSpecifications {
    public static Specification<Agent> hasNameAndNoCurrentTests(String name) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("name"), name),
                cb.equal(cb.size(root.get("currentTests")), 0)
        );
    }

    public static Specification<Agent> byIdAndNoCurrentTests(String id) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("id"), id),
                cb.equal(cb.size(root.get("currentTests")), 0)
        );
    }

}
