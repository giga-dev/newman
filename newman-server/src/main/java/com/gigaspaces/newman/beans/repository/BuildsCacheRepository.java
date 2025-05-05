package com.gigaspaces.newman.beans.repository;

import com.gigaspaces.newman.entities.Build;
import com.gigaspaces.newman.entities.BuildsCache;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BuildsCacheRepository extends CrudRepository<BuildsCache, String> {

    long count();

    BuildsCache findTopBy();

    @Transactional
    @Modifying
    @Query("UPDATE BuildsCache b SET b.cache = :cache, b.index = :index, b.size = :size WHERE b.id = :id")
    void updateBuildCache(@Param("id") String id, @Param("cache") List<Build> cache,
                          @Param("index") int index, @Param("size") int size);
}
