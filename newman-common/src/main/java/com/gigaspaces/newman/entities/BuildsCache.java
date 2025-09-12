package com.gigaspaces.newman.entities;

import javax.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Boris
 * @since 28/12/15
 * Cache builds representation in DB, this class assumes ONLY 1 thread is calling it!
 */
@Entity
@Table(name = "builds_cache")
public class BuildsCache {
    @Id
    private String id;
    private final int size = 10;
    private int index;

    @OneToMany
    private List<Build> cache;

    public BuildsCache() {
        this.cache = new ArrayList<>(size);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @param b build to insert to cache
     * @return the build that needs to be evicted and deleted from disk or null if there is still space left
     */

    public Build put(Build b) {
        if (cache.size() < size) {
            cache.add(b);
            return null;
        }

        Build replacedItem = cache.set(index, b);
        index = (index + 1) % size;
        return replacedItem;
    }

    public void remove(Build b) {
        if (cache.remove(b)) {
            index--;
        }
    }

    public boolean isInCache(Build b) {
        for (Build build : cache) {
            if (build != null && build.equals(b)) {
                return true;
            }
        }
        return false;
    }


    public List<Build> getCache() {
        return cache;
    }

    public void setCache(List<Build> cache) {
        this.cache = cache;
    }

    public int getSize() {
        return size;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("size", size)
                .append("index", index)
                .append("cache", cache)
                .toString();
    }
}
