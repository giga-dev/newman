package com.gigaspaces.newman.utils;

import org.hibernate.collection.internal.PersistentBag;

import java.util.Collection;
import java.util.List;

public class ConvertUtils {

    public static <T>Collection<T> unpackPersistentBag(Collection<T> collection) {
        if (collection instanceof PersistentBag && collection.size() == 1) {
            Object first = ((List<?>) collection).get(0);
            if (first instanceof List) {
                return (List<T>) first;
            }
        }
        return collection;
    }
}
