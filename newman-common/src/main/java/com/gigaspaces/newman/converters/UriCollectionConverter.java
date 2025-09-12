package com.gigaspaces.newman.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Converter
public class UriCollectionConverter implements AttributeConverter<Collection<URI>, String> {

    @Override
    public String convertToDatabaseColumn(Collection<URI> uris) {
        if (uris == null) {
            return null;
        }
        // Convert Collection<URI> to a comma-separated string
        return uris.stream()
                .map(URI::toString) // Convert each URI to string
                .collect(Collectors.joining(","));
    }

    @Override
    public Collection<URI> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Remove the curly braces { and }
        if (dbData.startsWith("{") && dbData.endsWith("}")) {
            dbData = dbData.substring(1, dbData.length() - 1);
        }

        if (dbData.startsWith("\"")) {
            dbData = dbData.replaceAll("\"", "");
        }
        // Convert comma-separated string back to Collection<URI>
        return Arrays.stream(dbData.split(","))
                .map(URI::create) // Convert each string back to URI
                .collect(Collectors.toList());
    }
}