package com.gigaspaces.newman.utils;

import java.util.Collection;
import java.util.Map;

/**
 * Created by moran
 * on 4/29/15.
 */
public class ToStringBuilder {
    private final String name;
    private final boolean ignoreNullValues;
    private StringBuilder builder;

    private ToStringBuilder(String name, boolean ignoreNullValues) {
        this.name = name;
        this.ignoreNullValues = ignoreNullValues;
        builder = new StringBuilder();
    }

    public static ToStringBuilder newBuilder(String name) {
        return new ToStringBuilder(name, false /*ignoreNullValues*/);
    }

    public static ToStringBuilder newBuilder(String name, boolean ignoreNullValues) {
        return new ToStringBuilder(name, ignoreNullValues);
    }

    public ToStringBuilder append(String key, Object value) {
        if (ignoreNullValues) {
            if (value == null) {
                return this;
            }
            if (value instanceof Collection) {
                if (((Collection) value).size() == 0) {
                    return this;
                }
            }
            if (value instanceof Map) {
                if (((Map) value).size() == 0) {
                    return this;
                }
            }
        }
        if (builder.length() > 0) {
            builder.append(',').append(' ');
        }
        builder.append(key).append(':').append(' ').append('\'').append(value).append('\'');
        return this;
    }

    @Override
    public String toString() {
        return name + " {" + builder + " }";
    }
}
