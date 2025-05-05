package com.gigaspaces.newman.beans.criteria;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.regex.Pattern;

/**
 * Regular expression matching criteria
 */
public class PatternCriteria implements Criteria {
    private String regex;

    public PatternCriteria() {
    }

    public PatternCriteria(String regex) {
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public static PatternCriteria recursivePackageNameCriteria(String packageName) {
        return new PatternCriteria(Pattern.quote(packageName) + "[.].*");
    }

    public static PatternCriteria nonRecursivePackageNameCriteria(String packageName) {
        return new PatternCriteria(Pattern.quote(packageName) + "[.]([^.]+)");
    }

    public static PatternCriteria classNameCriteria(String className) {
        return new PatternCriteria(Pattern.quote(className));
    }

    public static PatternCriteria startsWithCriteria(String string) {
        return new PatternCriteria(Pattern.quote(string) + ".*");
    }

    public static PatternCriteria endsWithCriteria(String string) {
        return new PatternCriteria(".*" + Pattern.quote(string));
    }

    public static PatternCriteria containsCriteria(String string) {
        return new PatternCriteria(".*" + Pattern.quote(string) + ".*");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("regex", regex)
                .toString();
    }
}
