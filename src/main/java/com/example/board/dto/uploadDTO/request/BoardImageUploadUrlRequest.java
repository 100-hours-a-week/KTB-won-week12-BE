package com.example.board.dto.uploadDTO.request;

import com.example.board.exception.errorMessage.ImageErrorMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BoardImageUploadUrlRequest(
        @NotEmpty(message = ImageErrorMessage.IMAGE_COUNT_LIMIT)
        @Size(max = 5, message = ImageErrorMessage.IMAGE_COUNT_LIMIT)
        List<@NotNull @Valid BoardImageUploadFileRequest> images
) {
}
