package com.gigaspaces.newman.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.*;

import java.io.IOException;
import java.util.Map;

@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting Map to JSON", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null || dbData.isEmpty() ? null : objectMapper.readValue(dbData, Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to Map", e);
        }
    }
}
