package com.gigaspaces.newman.analytics;

import java.util.Properties;

/**
 * Created by moran on 8/19/15.
 */
public class NewmanAnalytics {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            throw new IllegalArgumentException("Illegal usage; expected: <properties-file-path> <optionally followed by key=value args>");
        }

        Properties properties = new PropertiesConfigurer(args).getProperties();

        String cronableClass = properties.getProperty("cronable-class");
        if (cronableClass == null) {
            throw new IllegalArgumentException("No cronable-class class defined in " + args[0]);
        }

        Cronable cronable = Class.forName(cronableClass).asSubclass(Cronable.class).newInstance();
        cronable.run(properties);
    }
}
