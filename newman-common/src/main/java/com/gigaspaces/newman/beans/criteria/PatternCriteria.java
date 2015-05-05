package com.gigaspaces.newman.beans.criteria;

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
        String regex = packageName.replaceAll("\\.", "[.]");
        return new PatternCriteria(regex + "[.].*");
    }

    public static PatternCriteria nonRecursivePackageNameCriteria(String packageName) {
        String regex = packageName.replaceAll("\\.", "[.]");
        return new PatternCriteria(regex+"[.]([^.]+)");
    }

    public static PatternCriteria classNameCriteria(String className) {
        String regex = className.replaceAll("\\.", "[.]");
        return new PatternCriteria(regex);
    }

}
