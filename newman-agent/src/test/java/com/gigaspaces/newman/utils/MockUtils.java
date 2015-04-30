package com.gigaspaces.newman.utils;

import com.gigaspaces.newman.beans.Build;
import com.gigaspaces.newman.beans.Job;
import com.gigaspaces.newman.beans.State;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;


public class MockUtils {

    public static Build createMockBuild() {
        Build build = new Build();
        build.setId(UUID.randomUUID().toString());
        build.setBranch("master");

        HashMap<String, String> shas = new HashMap<>();
        shas.put("master", "e75126068d6cbd7f6ec5231b6ae7429ea89e4ee8");
        build.setShas(shas);

        URI artifactsURI = URI.create("https://s3-eu-west-1.amazonaws.com/gigaspaces-repository-eu/com/gigaspaces/xap-core/newman/newman-artifacts.zip");
        Collection<URI> collection = new ArrayList<>();
        collection.add(artifactsURI);
        build.setResources(collection);

        return build;
    }

    public static Job createMockJob() {
        Job j = new Job();
        j.setId(UUID.randomUUID().toString());
        j.setSubmittedBy("mock");
        j.setState(State.READY);

        Build b = new Build();
        j.setBuild(b);

        return j;
    }
}
