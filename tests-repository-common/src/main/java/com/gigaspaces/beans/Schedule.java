package com.gigaspaces.beans;

import org.mongodb.morphia.annotations.Entity;

import java.time.LocalDateTime;

/**
 * Created by Barak Bar Orion
 * 4/13/15.
 */
@Entity
public class Schedule {
    private LocalDateTime scheduleTime;

}
