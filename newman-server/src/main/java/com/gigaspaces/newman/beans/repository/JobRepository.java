package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.beans.State;
import com.gigaspaces.newman.entities.Job;
import com.gigaspaces.newman.projections.PJobForDashboard;
import com.gigaspaces.newman.projections.PJobThin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends CrudRepository<Job, String>, JpaSpecificationExecutor<Job>, PagingAndSortingRepository<Job, String>, JpaRepository<Job, String> {

    @Query("SELECT j.id AS id, j.suiteId AS suiteId, j.suiteName AS suiteName, " +
            "j.build.id AS buildId, j.build.name AS buildName, j.build.branch AS buildBranch " +
            "FROM Job j WHERE j.id = :jobId")
    PJobThin findOneThinJobById(@Param("jobId") String jobId);

    Page<Job> findByBuildId(String buildId, Pageable pageable);

    @Query("SELECT COUNT(j) FROM Job j WHERE (j.state = 'READY' OR j.state = 'RUNNING') AND j.totalTests > 0")
    Long countReadyAndRunningJobs();

    @Query("SELECT j FROM Job j " +
        "WHERE j.suiteId = :suiteId AND j.state = 'DONE'")
    List<Job> findTopJobsForSuiteDoneState(@Param("suiteId") String suiteId, Pageable pageable);

    @Query("SELECT j.id AS id FROM Job j WHERE j.suiteId = :suiteId")
    List<String> findJobIdsBySuiteId(@Param("suiteId") String suiteId);

    List<Job> findByState(State paused);

    @Transactional
    @Modifying
    @Query("UPDATE Job j " +
        "SET j.state = 'READY', j.startPrepareTime = NULL " +
        "WHERE j.id = :jobId AND j.state = 'RUNNING' AND j.runningTests <= 0 ")
    int updateJobToReadyIfNoTestsRunning(@Param("jobId") String jobId);

    @Query("SELECT j FROM Job j WHERE j.submittedBy = :submittedBy AND FUNCTION('DATE',j.submitTime) = :requestedDate")
    List<Job> findBySubmittedByAndSubmitTime(@Param("submittedBy") String submittedBy,
                                             @Param("requestedDate") Date requestedDate);

    List<Job> findBySubmitTimeBeforeAndStateNot(Date deleteUntilDate, State state);

    List<Job> findAllByBuildId(String buildId);

    @Transactional
    @Modifying
    @Query("UPDATE Job j SET j.state = :newState WHERE j.state = :currState1 OR j.state = :currState2")
    int updateJobsState(@Param("newState") State newState,
                        @Param("currState1") State currState1,
                        @Param("currState2") State currState2);

    List<Job> findByStateAndRunningTestsGreaterThan(State state, int runningTestsThreshold);

    Optional<Job> findByIdAndStateNot(String jobId, State state);

    List<PJobForDashboard> findByBuildIdAndState(String buildId, State state);
}
