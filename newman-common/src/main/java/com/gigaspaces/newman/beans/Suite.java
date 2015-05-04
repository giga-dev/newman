package com.gigaspaces.newman.beans;

import com.gigaspaces.newman.beans.criteria.Criteria;
import com.gigaspaces.newman.beans.utils.ToStringBuilder;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.net.URI;
import java.util.*;

/**
 * Created by moran
 * on 4/29/15.
 */
@Entity
public class Suite {
    @Id
    private String id;

    @Embedded
    private List<Criteria> criterias;

    public Suite() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Criteria> getCriterias() {
        return criterias;
    }

    public void setCriterias(List<Criteria> criterias) {
        this.criterias = criterias;
    }

    @Override
    public String toString() {
        return ToStringBuilder.newBuilder(this.getClass().getSimpleName())
                .append("id", id)
                .append("criterias", criterias)
                .toString();
    }
}
