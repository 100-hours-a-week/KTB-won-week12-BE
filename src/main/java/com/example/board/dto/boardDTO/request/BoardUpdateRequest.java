package com.example.board.dto.boardDTO.request;

import com.example.board.exception.errorMessage.BoardErrorMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BoardUpdateRequest(
        @NotBlank(message = BoardErrorMessage.TITLE_REQUIRED)
        @Size(max = 26, message = BoardErrorMessage.TITLE_LENGTH_LIMIT)
        String title,

        @NotBlank(message = BoardErrorMessage.CONTENT_REQUIRED)
        String content,

        List<
                @NotBlank(message = BoardErrorMessage.IMAGE_URL_REQUIRED)
                @Size(max = 2048, message = BoardErrorMessage.IMAGE_URL_LENGTH_LIMIT)
                @Pattern(regexp = "^https?://.+", message = BoardErrorMessage.IMAGE_URL_INVALID)
                String> imageUrls
) {
    public List<String> safeImageUrls() {
        return imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
