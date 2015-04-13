package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.List;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Suite {
    private List<Test> descriptions;
}
