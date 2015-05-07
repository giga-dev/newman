package com.gigaspaces.newman.utils.setup;

import javax.validation.constraints.NotNull;

/**
 * Created by Barak Bar Orion
 * 5/7/15.
 * Holds metadata of a version.
 */
public class MockSetupProperties {
    private final String name;

    public MockSetupProperties(@NotNull String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MockSetupProperties that = (MockSetupProperties) o;

        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "SetupProperties{" +
                "name='" + name + '\'' +
                '}';
    }
}
