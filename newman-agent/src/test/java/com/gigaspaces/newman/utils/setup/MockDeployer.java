package com.gigaspaces.newman.utils.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Barak Bar Orion
 * 5/7/15.
 */
public class MockDeployer implements SetupDeployer<MockSetupProperties, MockSetup> {
    public final ConcurrentHashMap<MockSetupProperties, MockSetup> deployed;

    public MockDeployer() {
        this.deployed = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized MockSetup deploy(MockSetupProperties properties) {
        MockSetup setup = new MockSetup(properties);
        MockSetup old = deployed.putIfAbsent(properties, setup);
        if(old != null){
            throw new IllegalStateException("setup " + properties + " already deployed " + deployed.keySet());
        }
        return setup;
    }

    @Override
    public void undeploy(MockSetup setup) {
        MockSetup removed = deployed.remove(setup.getProperties());
        if(removed == null){
            throw new IllegalStateException("setup " + setup + " can not removed since it not deployed");
        }
    }

    public Map<MockSetupProperties, MockSetup> getDeployed() {
        return deployed;
    }
}
