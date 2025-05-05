package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.FutureJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FutureJobRepository extends CrudRepository<FutureJob, String> {

    Optional<FutureJob> findFirstByOrderBySubmitTimeAsc();

    List<FutureJob> findAll();
}
