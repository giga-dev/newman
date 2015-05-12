package com.gigaspaces.newman;

/**
 @author Boris
 @since 1.0
 */

public class NewmanTestsMetadataParserFactory {

    public static NewmanTestsMetadataParser create(String type){

        if (type.equals("tgrid")){
            return new TgridTestsMetadataParser();
        }
        else if (type.equals("sgtest")){
            throw new UnsupportedOperationException("sgtest metadata is not supported yet");
        }
        else {
            throw new IllegalArgumentException("wrong type of metadata " + type);
        }
    }
}
