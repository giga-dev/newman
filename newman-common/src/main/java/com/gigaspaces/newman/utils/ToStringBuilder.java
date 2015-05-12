package com.gigaspaces.newman.utils;

/**
 * Created by moran on 4/29/15.
 */
public class ToStringBuilder {
    private final String name;
    private StringBuilder builder;

    private ToStringBuilder(String name) {
        this.name = name;
        builder = new StringBuilder();
    }

    public static ToStringBuilder newBuilder(String name) {
        return new ToStringBuilder(name);
    }

    public ToStringBuilder append(String key, Object value) {
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
