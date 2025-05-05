package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.PrioritizedJob;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrioritizedJobRepository extends CrudRepository<PrioritizedJob, String> {

    @Query("SELECT pj FROM PrioritizedJob pj WHERE pj.isPaused = false ORDER BY pj.priority DESC")
    Optional<PrioritizedJob> findTopByNotPausedOrderByPriorityDesc();

    Optional<PrioritizedJob> findPrioritizedJobByJobId(String jobId);

    List<PrioritizedJob> findByIsPausedFalseOrderByPriorityDesc();
}