package com.gigaspaces.newman.converters;

import javax.persistence.*;

import java.net.URI;

@Converter(autoApply = true)
public class UriToStringConverter implements AttributeConverter<URI, String> {

    @Override
    public String convertToDatabaseColumn(URI attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public URI convertToEntityAttribute(String dbData) {
        return dbData != null ? URI.create(dbData) : null;
    }
}

