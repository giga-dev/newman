package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by moran
 * on 4/29/15.
 */
@Entity
public class Suite {
    @Id
    private String id;

    private String name;

    @Embedded
    private Criteria criteria;

    public Suite() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("name", name)
                .append("criteria", criteria)
                .toString();
    }
}
