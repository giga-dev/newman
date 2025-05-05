package com.gigaspaces.newman;

import com.gigaspaces.newman.entities.Test;
import org.json.simple.JSONObject;

import java.util.List;

/**
 @author Boris
 @since 1.0
 */

public interface NewmanTestsMetadataParser {
    List<Test> parse(JSONObject metadata);
}
