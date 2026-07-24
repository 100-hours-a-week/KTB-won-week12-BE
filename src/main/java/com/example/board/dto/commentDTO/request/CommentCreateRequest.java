package com.example.board.dto.commentDTO.request;

import com.example.board.exception.errorMessage.CommentErrorMessage;
import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank(message = CommentErrorMessage.CONTENT_REQUIRED)
        String content
) {
}
