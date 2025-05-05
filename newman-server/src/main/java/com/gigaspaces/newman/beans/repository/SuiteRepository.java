package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.Suite;
import com.gigaspaces.newman.projections.PSuiteThin;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface SuiteRepository extends CrudRepository<Suite, String> {
    @Query(value = "SELECT DISTINCT unnest(requirements) FROM suite", nativeQuery = true)
    Set<String> findAllRequirements();

    boolean existsByName(String name);

    @Query("SELECT s.id AS id, s.name AS name, s.customVariables AS customVariables FROM Suite s ORDER BY s.name")
    List<PSuiteThin> findAllThinWithCustomVariablesOrderedByName();

    @Query("SELECT s.id AS id, s.name AS name FROM Suite s ORDER BY s.name")
    List<PSuiteThin> findAllThinNoCustomVariablesOrderedByName();

}
