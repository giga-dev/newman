package com.gigaspaces.beans;

import com.gigaspaces.componenets.TestsRepositoryIfc;
import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinkNoFollow;
import org.mongodb.morphia.annotations.Transient;

import java.net.URI;
import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/12/15.
 */
public class Batch<T> {

    @InjectLinkNoFollow
    private List<T> values;
    private int offset;
    private int limit;

    @Transient
    @InjectLink(resource = TestsRepositoryIfc.class,
            method = "ls",
            bindings = {
                    @Binding(name = "offset", value = "${instance.offset}"),
                    @Binding(name = "limit", value = "${instance.limit}")
            },
            style = InjectLink.Style.ABSOLUTE)
    private URI self;

    @Transient
    @InjectLink(resource = TestsRepositoryIfc.class,
            method = "ls",
            bindings = {
                    @Binding(name = "offset", value = "${instance.offset - instance.limit}"),
                    @Binding(name = "limit", value = "${instance.limit}")
            },
            condition="${0 < instance.offset - instance.limit}",
            style = InjectLink.Style.ABSOLUTE)
    private URI back;
    @Transient
    @InjectLink(resource = TestsRepositoryIfc.class,
            method = "ls",
            bindings = {
                    @Binding(name = "offset", value = "${instance.offset + instance.limit}"),
                    @Binding(name = "limit", value = "${instance.limit}")
            },
            style = InjectLink.Style.ABSOLUTE)
    private URI next;

    public Batch() {
    }

    public Batch(List<T> ids, int offset, int limit) {
        this.values = ids;
        this.offset = offset;
        this.limit = limit;
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

    public int getLimit() {
        return limit;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

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
        return "Batch{" +
                "values=" + values +
                ", offset=" + offset +
                ", limit=" + limit +
                ", self=" + self +
                ", back=" + back +
                ", next=" + next +
                '}';
    }
}
