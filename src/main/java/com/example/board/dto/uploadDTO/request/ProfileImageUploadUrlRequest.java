package com.example.board.dto.uploadDTO.request;

import com.example.board.exception.errorMessage.ImageErrorMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프로필 원본 이미지를 S3에 직접 업로드하기 위해 클라이언트가 전달하는 메타데이터다.
 */
public record ProfileImageUploadUrlRequest(
        @NotBlank(message = ImageErrorMessage.FILE_NAME_REQUIRED)
        @Size(max = 255, message = ImageErrorMessage.FILE_NAME_LENGTH_LIMIT)
        String fileName,
        String contentType,
        long size
) {
}
