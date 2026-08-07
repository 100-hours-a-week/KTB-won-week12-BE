package com.example.board.dto.boardDTO.response;

import java.time.LocalDateTime;
import java.util.List;

public record BoardDetailResponse(
        Long boardId,
        String title,
        String content,
        List<BoardImageResponse> images,
        BoardAuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int likeCount,
        int viewCount,
        long commentCount,
        boolean likedByMe,
        boolean editableByMe,
        // 투표를 생성하지 않은 게시글은 JSON에서 vote: null로 반환한다.
        BoardVoteDetailResponse vote
) {
}
