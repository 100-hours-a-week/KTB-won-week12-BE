package com.example.board.dto.boardDTO.response;

import java.util.List;

public record BoardCursorResponse(
        List<BoardSummaryResponse> content,
        Long nextCursor,
        boolean hasNext
) {
}
