package com.example.board.dto.commentDTO.response;

import java.util.List;

public record CommentCursorResponse(
        List<CommentResponse> content,
        Long nextCursor,
        boolean hasNext
) {
}
