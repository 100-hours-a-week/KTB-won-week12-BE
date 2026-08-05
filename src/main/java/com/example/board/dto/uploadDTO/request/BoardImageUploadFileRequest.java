package com.example.board.dto.uploadDTO.request;

import com.example.board.exception.errorMessage.ImageErrorMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardImageUploadFileRequest(
        @NotBlank(message = ImageErrorMessage.FILE_NAME_REQUIRED)
        @Size(max = 255, message = ImageErrorMessage.FILE_NAME_LENGTH_LIMIT)
        String originalFileName,
        String originalContentType,
        long originalSize,
        String thumbnailContentType,
        long thumbnailSize
) {
}
