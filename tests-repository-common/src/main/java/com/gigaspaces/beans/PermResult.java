package com.gigaspaces.beans;

import com.gigaspaces.componenets.TestsRepositoryIfc;
import org.glassfish.jersey.linking.InjectLink;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import java.net.URI;
import java.util.Map;

/**
 * Created by Barak Bar Orion
 * 4/11/15.
 */

@Entity("perm-result")
public class PermResult {
    @Id
    private String id;
    private String permutation;
    private Map<String, String> logs;

    @Transient
    @InjectLink(resource = TestsRepositoryIfc.class, method = "get", value = "{id}", style = InjectLink.Style.ABSOLUTE)
    private URI self;

    public PermResult() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPermutation() {
        return permutation;
    }

    public void setPermutation(String permutation) {
        this.permutation = permutation;
    }

    public Map<String, String> getLogs() {
        return logs;
    }

    public void setLogs(Map<String, String> logs) {
        this.logs = logs;
    }

    public URI getSelf() {
        return self;
    }

    public void setSelf(URI self) {
        this.self = self;
    }

    @Override
    public String toString() {
        return "PermResult{" +
                "id='" + id + '\'' +
                ", permutation='" + permutation + '\'' +
                ", logs=" + logs +
                ", self=" + self +
                '}';
    }
}
