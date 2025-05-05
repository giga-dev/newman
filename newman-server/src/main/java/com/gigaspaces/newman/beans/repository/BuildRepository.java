package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.Build;
import com.gigaspaces.newman.projections.PBuildForView;
import com.gigaspaces.newman.projections.PBuildThin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BuildRepository extends CrudRepository<Build, String>, JpaRepository<Build, String>, JpaSpecificationExecutor<Build>, PagingAndSortingRepository<Build, String> {

    @Query("SELECT b FROM Build b")
    Page<PBuildThin> findThinBuilds(Pageable pageable);

    @Query("SELECT b FROM Build b")
    Page<PBuildForView> findAllForViewList(Pageable pageable);

    @Query("SELECT b FROM Build b WHERE b.branch = :branch ORDER BY b.buildTime DESC")
    Build findLatestBuildByBranch(@Param("branch") String branch);

    Build findByName(String name);

    @Query("SELECT b FROM Build b " +
            "WHERE :tag1 MEMBER OF b.tags AND " +
            ":tag2 MEMBER OF b.tags " +
            "ORDER BY b.buildTime DESC")
    List<Build> findBuildsWithTags(String tag1, String tag2);

    List<Build> findByBuildTimeBefore(Date beforeDate);

    @Query("SELECT b FROM Build b WHERE b.buildStatus.runningJobs > 0 " +
            "AND b.buildStatus.totalJobs > 0 " +
            "AND (b.buildStatus.doneJobs + b.buildStatus.brokenJobs) < b.buildStatus.totalJobs " +
            "ORDER BY b.buildTime DESC")
    List<Build> findActiveBuildsDescByBuildTime();

    @Query("SELECT b FROM Build b WHERE b.buildStatus.pendingJobs > 0 " +
            "AND b.buildStatus.runningJobs <= 0 " +
            "AND b.buildStatus.totalJobs > 0 " +
            "AND (b.buildStatus.doneJobs + b.buildStatus.brokenJobs) < b.buildStatus.totalJobs " +
            "ORDER BY b.buildTime DESC")
    List<Build> findPendingBuildsDescByBuildTime(Pageable pageable);

    @Query("SELECT b FROM Build b WHERE b.buildStatus.totalJobs > 0 " +
            "AND (b.buildStatus.doneJobs + b.buildStatus.brokenJobs) = b.buildStatus.totalJobs " +
            "ORDER BY b.buildTime DESC")
    List<Build> findRecentlyCompletedBuildsDescByBuildTime(Pageable pageable);
}
