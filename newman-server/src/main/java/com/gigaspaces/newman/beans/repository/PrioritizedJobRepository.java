package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.PrioritizedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrioritizedJobRepository extends CrudRepository<PrioritizedJob, String>, JpaRepository<PrioritizedJob, String> {

    Optional<PrioritizedJob> findTopByIsPausedFalseOrderByPriorityDesc();

    List<PrioritizedJob> findByIsPausedFalseOrderByPriorityDesc();

    @Transactional
    @Modifying
    void deleteByJobId(String jobId);

    Optional<PrioritizedJob> findByJobId(String jobId);
}