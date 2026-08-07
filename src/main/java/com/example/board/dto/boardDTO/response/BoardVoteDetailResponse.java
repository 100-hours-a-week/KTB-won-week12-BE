package com.example.board.dto.boardDTO.response;

import com.example.board.domain.board.BoardVoteStatus;

import java.time.LocalDateTime;

public record BoardVoteDetailResponse(
        Long voteId,
        String leftLabel,
        String rightLabel,
        BoardVoteStatus status,
        LocalDateTime startedAt,
        LocalDateTime endsAt,
        long totalVoteCount,
        BoardVoteResultResponse result,
        BoardVoteMyResponse myVote
) {
}
