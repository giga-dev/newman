package com.gigaspaces.newman.utils;

import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.State;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;


public class MockUtils {

    public static Job createMockJob() {
        Job j = new Job();
        URI artifactsURI = URI.create("https://s3-eu-west-1.amazonaws.com/gigaspaces-repository-eu/com/gigaspaces/xap-core/newman/newman-artifacts.zip");
        Collection<URI> collection = new ArrayList<>();
        collection.add(artifactsURI);
        j.setResources(collection);
        j.setId(UUID.randomUUID().toString());
        j.setSubmittedBy("mock");
        j.setState(State.READY);

        return j;
    }
}
