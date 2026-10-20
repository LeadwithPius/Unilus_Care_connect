package com.unilus.careconnect.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EducationResourceResponse(
        UUID id,
        String slug,
        String title,
        String topic,
        String summary,
        String content,
        String contentType,
        String resourceUrl,
        boolean published,
        int displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
