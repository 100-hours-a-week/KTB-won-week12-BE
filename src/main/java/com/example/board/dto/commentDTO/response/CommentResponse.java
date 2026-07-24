package com.example.board.dto.commentDTO.response;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        String content,
        CommentAuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean editableByMe
) {
}
