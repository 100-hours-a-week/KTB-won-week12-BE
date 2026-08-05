package com.example.board.dto.boardDTO.request;

import com.example.board.domain.board.BoardImageKeys;
import com.example.board.exception.errorMessage.ImageErrorMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardImageKeyRequest(
        @NotBlank(message = ImageErrorMessage.OBJECT_KEY_REQUIRED)
        @Size(max = 512, message = ImageErrorMessage.OBJECT_KEY_LENGTH_LIMIT)
        String originalObjectKey,

        @NotBlank(message = ImageErrorMessage.OBJECT_KEY_REQUIRED)
        @Size(max = 512, message = ImageErrorMessage.OBJECT_KEY_LENGTH_LIMIT)
        String thumbnailObjectKey
) {
    public BoardImageKeys toDomain() {
        return new BoardImageKeys(originalObjectKey, thumbnailObjectKey);
    }
}
