package com.ecommerce.order.support;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.TreeMap;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, String>, String> {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<TreeMap<String, String>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        return MAPPER.writeValueAsString(attribute == null ? Map.of() : new TreeMap<>(attribute));
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? new TreeMap<>() : MAPPER.readValue(dbData, TYPE);
    }
}
