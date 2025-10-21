package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.Test;
import com.gigaspaces.newman.projections.PTest;
import com.gigaspaces.newman.projections.PTestForHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TestRepository extends CrudRepository<Test, String>, JpaSpecificationExecutor<Test>, JpaRepository<Test, String> {

    @Query("SELECT DISTINCT t.assignedAgent FROM Test t WHERE t.jobId = :jobId")
    Set<String> findDistinctAssignedAgentByJobId(@Param("jobId") String jobId);

    @Transactional
    @Modifying
    void deleteByJobId(String jobId);

    List<Test> findByJobIdAndStatusAndRunNumber(String jobId, Test.Status status, int runNumber);

    boolean existsByJobIdAndStatusIn(String jobId, Collection<Test.Status> status);

    @Transactional
    @Modifying
    @Query("UPDATE Test t SET t.status = :newStatus WHERE t.jobId = :jobId AND t.status = :currentStatus")
    int updateStatusByJobIdAndCurrentStatus(@Param("jobId") String jobId,
                                            @Param("currentStatus") Test.Status currentStatus,
                                            @Param("newStatus") Test.Status newStatus);

    Optional<Test> findFirstByJobIdAndStatus(String jobId, Test.Status status);

    @Query("SELECT t FROM Test t WHERE t.assignedAgent = :assignedAgent")
    List<PTest> findAllByAssignedAgent(@Param("assignedAgent") String assignedAgent, Pageable pageable);

    @Query("SELECT t FROM Test t WHERE t.jobId = :jobId")
    List<PTest> findAllByJobId(@Param("jobId") String jobId, Pageable pageable);

    @Query(value = "SELECT " +
                "t.name AS name, " +
                "t.endtime AS endTime, " +
                "array_to_string(t.arguments, ' ') AS arguments, " +
                "t.sha AS sha, " +
                "b.branch AS branch, " +
                "j.id as jobId, " +
                "b.id AS buildId, " +
                "s.id AS suiteId " +
            "FROM test t " +
            "LEFT JOIN job j ON j.id = t.jobid " +
            "JOIN suite s ON s.id = j.suite_id " +
            "JOIN build b ON b.id = j.build_id " +
            "WHERE t.id = :id", nativeQuery = true)
    Optional<PTestForHistory> findTestForHistoryById(@Param("id") String id);
}
