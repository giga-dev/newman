package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.Test;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 @author Boris
 @since 1.0
 */

public class TgridTestsMetadataParser implements NewmanTestsMetadataParser {

    @Override
    public List<Test> parse(JSONObject metadata) {
        List<Test> parsedTests = new ArrayList<>();
        JSONArray tests = (JSONArray) metadata.get("tests");
        for (Object jsonTest : tests) {
            JSONObject jTest = (JSONObject) jsonTest;
            Test test = new Test();
            test.setName((String) jTest.get("name"));
            test.setTestType("tgrid");
            List<String> arguments = new ArrayList<>();
            //TODO break arguments into parts if needed
            arguments.add((String) jTest.get("arguments"));
            test.setArguments(arguments);
            //noinspection unchecked
            test.setProperties((Map<String, String>) jTest.get("annotations"));
            parsedTests.add(test);
        }
        return parsedTests;
    }
}
