package com.example.board.dto.boardDTO.response;

import com.example.board.domain.board.BoardVoteStatus;

import java.time.LocalDateTime;

public record BoardVoteUpdateResponse(
        Long voteId,
        BoardVoteStatus status,
        LocalDateTime endsAt,
        long totalVoteCount,
        BoardVoteResultResponse result,
        BoardVoteMyResponse myVote
) {
}
