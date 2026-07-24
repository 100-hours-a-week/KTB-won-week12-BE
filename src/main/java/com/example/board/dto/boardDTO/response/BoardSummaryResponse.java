package com.example.board.dto.boardDTO.response;

import java.time.LocalDateTime;

public record BoardSummaryResponse(
        Long boardId,
        String title,
        BoardAuthorResponse author,
        LocalDateTime createdAt,
        int likeCount,
        long commentCount,
        int viewCount
) {
}
