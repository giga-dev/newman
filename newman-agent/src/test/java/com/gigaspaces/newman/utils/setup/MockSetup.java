package com.gigaspaces.newman.utils.setup;

/**
 * Created by Barak Bar Orion
 * 5/7/15.
 * Represent the metadata of an *installed* version for this test.
 */
public class MockSetup {
    private MockSetupProperties properties;

    public MockSetup(MockSetupProperties properties) {
        this.properties = properties;
    }

    public MockSetupProperties getProperties() {
        return properties;
    }

    public void setProperties(MockSetupProperties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "MockSetup{" +
                "properties=" + properties +
                '}';
    }
}
