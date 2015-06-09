package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Test;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 @author Boris
 @since 1.0
 */

public class TgridAndSGTestTestsMetadataParser implements NewmanTestsMetadataParser {

    private String type;

    public TgridAndSGTestTestsMetadataParser(String type) {
        this.type = type;
    }

    @Override
    public List<Test> parse(JSONObject metadata) {
        List<Test> parsedTests = new ArrayList<>();
        JSONArray tests = (JSONArray) metadata.get("tests");
        for (Object jsonTest : tests) {
            JSONObject jTest = (JSONObject) jsonTest;
            Test test = new Test();
            //TODO figure out the timeout per test
            test.setTimeout((long) (15 * 60 * 1000));
            test.setName((String) jTest.get("name"));
            test.setTestType(type);
            List<String> arguments = new ArrayList<>();
            //noinspection unchecked
            arguments.addAll((Collection<? extends String>) jTest.get("arguments"));
            test.setArguments(arguments);
            //noinspection unchecked
            test.setProperties((Map<String, String>) jTest.get("annotations"));
            parsedTests.add(test);
        }
        return parsedTests;
    }
}
