package com.gigaspaces.newman.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * @author Boris
 * @since 28/12/15
 * Cache builds representation in DB, this class assumes ONLY 1 thread is calling it!
 */
@Entity
public class BuildsCache {
    @Id
    private String id;
    private final int size = 10;
    private Build[] cache;
    private int index;

    public BuildsCache() {
        this.cache = new Build[size];
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

        Build res = cache[index % size];
        cache[index % size] = b;
        index++;
        return res;
    }

    public void remove(Build b) {
        for (int i=0; i < size; i++){
            if (cache[i] != null && cache[i].equals(b)) {
                cache[i] = null;
            }
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


    public Build[] getCache() {
        return cache;
    }

    public void setCache(Build[] cache) {
        this.cache = cache;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "BuildsCache{" +
                "id='" + id + '\'' +
                ", cache=" + cache +
                ", size=" + size +
                '}';
    }
}
