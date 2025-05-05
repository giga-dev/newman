package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.JobConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobConfigRepository extends CrudRepository<JobConfig, String> {
    Optional<JobConfig> findByName(String name);

    List<JobConfig> findAll();
}
