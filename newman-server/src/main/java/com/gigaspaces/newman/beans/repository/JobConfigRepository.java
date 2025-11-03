package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.JobConfig;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobConfigRepository extends CrudRepository<JobConfig, String> {
    Optional<JobConfig> findByName(String name);

    List<JobConfig> findAll();

    @Modifying
    @Transactional
    @Query("UPDATE JobConfig j SET j.isDefault = false WHERE j.isDefault = true")
    void unsetAllDefaults();
}
