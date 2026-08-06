package com.example.board.dto.uploadDTO.response;

import java.time.Instant;

/**
 * 브라우저가 S3에 PUT할 때 필요한 URL과 이후 회원정보 수정에 전달할 Object Key다.
 */
public record ProfileImageUploadUrlResponse(
        String objectKey,
        String uploadUrl,
        String contentType,
        Instant expiresAt
) {
}
