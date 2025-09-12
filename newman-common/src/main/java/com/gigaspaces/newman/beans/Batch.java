package com.gigaspaces.newman.beans;

import javax.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/12/15.
 */
public class Batch<T> {

    private List<T> values;
    private int offset;
    private int limit;
    @Transient
    private URI back;
    @Transient
    private URI self;
    @Transient
    private URI next;

    @SuppressWarnings("unused")
    public Batch() {
    }

    public Batch(List<T> values, int offset, int limit, boolean all, List<String> orderBy, UriInfo uriInfo) {
        this.values = values;
        this.offset = offset;
        this.limit = limit;
        if (uriInfo == null){
            return;
        }
        UriBuilder selfBuilder = uriInfo.getAbsolutePathBuilder();
        UriBuilder backBuilder = uriInfo.getAbsolutePathBuilder();
        UriBuilder nextBuilder = uriInfo.getAbsolutePathBuilder();
        if(orderBy != null){
            for (String ob : orderBy) {
                selfBuilder.queryParam("orderBy", ob);
                backBuilder.queryParam("orderBy", ob);
                nextBuilder.queryParam("orderBy", ob);
            }
        }
        if(all){
            this.self = selfBuilder.queryParam("all", true).build();
        }else {
            if (0 < offset) {
                this.back = backBuilder.queryParam("offset", Math.max(0, offset - limit)).queryParam("limit", limit).build();
            }
            this.self = selfBuilder.queryParam("offset", offset).queryParam("limit", limit).build();
            this.next = nextBuilder.queryParam("offset", offset + limit).queryParam("limit", limit).build();
        }
    }


    public List<T> getValues() {
        return values;
    }

    public void setValues(List<T> values) {
        this.values = values;
    }

    public int getOffset() {
        return offset;
    }

    @SuppressWarnings("unused")
    public int getLimit() {
        return limit;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @SuppressWarnings("unused")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    public URI getSelf() {
        return self;
    }

    public void setSelf(URI self) {
        this.self = self;
    }

    public URI getBack() {
        return back;
    }

    public void setBack(URI back) {
        this.back = back;
    }

    public URI getNext() {
        return next;
    }

    public void setNext(URI next) {
        this.next = next;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("values", values)
                .append("offset", offset)
                .append("limit", limit)
                .append("back", back)
                .append("self", self)
                .append("next", next)
                .toString();
    }
}
