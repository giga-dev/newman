package com.gigaspaces.newman;

import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tamirt
 * on 8/27/15.
 */
public class SizeParser {
    private String size;

    public SizeParser(String sizeString) {
        size = sizeString;
    }

    public long parse() throws IllegalFormatException {
        Pattern pattern = Pattern.compile("([0-9]+)(G|M)");
        Matcher matcher = pattern.matcher(size);
        if (matcher.matches()) {
            long value = Long.valueOf(matcher.group(1));
            String unit = matcher.group(2);
            if (unit.equals("M")) {
                value = value * 1000000;
            } else {
                value = value * 1000000000;
            }
            return value;
        } else {
           throw new IllegalArgumentException(); //didn't find a match
        }

    }
}
