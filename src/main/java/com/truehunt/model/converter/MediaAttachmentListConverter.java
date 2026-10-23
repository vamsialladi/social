package com.truehunt.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truehunt.model.MediaAttachment;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class MediaAttachmentListConverter implements AttributeConverter<List<MediaAttachment>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<MediaAttachment> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<MediaAttachment> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<MediaAttachment>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
