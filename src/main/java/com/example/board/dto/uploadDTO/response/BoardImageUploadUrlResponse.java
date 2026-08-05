package com.example.board.dto.uploadDTO.response;

import java.time.Instant;

public record BoardImageUploadUrlResponse(
        String originalObjectKey,
        String originalUploadUrl,
        String originalContentType,
        String thumbnailObjectKey,
        String thumbnailUploadUrl,
        String thumbnailContentType,
        Instant expiresAt
) {
}
