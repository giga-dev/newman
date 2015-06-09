package com.gigaspaces.newman;

/**
 @author Boris
 @since 1.0
 */

public class NewmanTestsMetadataParserFactory {

    public static NewmanTestsMetadataParser create(String type){
        return new TgridAndSGTestTestsMetadataParser(type);
    }
}
