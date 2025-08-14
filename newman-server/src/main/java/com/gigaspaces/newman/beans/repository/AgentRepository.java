package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public interface AgentRepository extends CrudRepository<Agent, String>, JpaRepository<Agent, String>, JpaSpecificationExecutor<Agent>, PagingAndSortingRepository<Agent, String> {
    @Query("SELECT DISTINCT a.groupName FROM Agent a")
    List<String> findDistinctAgentGroups();

    long countBySetupRetriesGreaterThan(int zero);

    Optional<Agent> findByName(String name);

    @Query("SELECT a FROM Agent a WHERE a.state <> :state AND a.lastTouchTime < :timeThreshold")
    List<Agent> findAgentsNotSeenInLastMillis(@Param("state") Agent.State idling, @Param("timeThreshold") Date timeThreshold);

    @Query("SELECT a FROM Agent a WHERE a.state = :state AND a.lastTouchTime < :timeThreshold")
    List<Agent> findZombieAgents(@Param("state") Agent.State idling, @Param("timeThreshold") Date timeThreshold);

    @Query("SELECT a FROM Agent a WHERE a.setupRetries > 0")
    Page<Agent> findAllWithPositiveSetupRetries(Pageable pageable);

    @Query("SELECT a FROM Agent a WHERE a.setupRetries > 0")
    List<Agent> findAllWithPositiveSetupRetries(Sort sort);

    @Transactional
    @Modifying
    @Query("UPDATE Agent a SET a.jobId = null WHERE a.name = :name")
    int unsetJobIdByName(@Param("name") String name);

    int countAgentsByGroupName(String groupName);
}
