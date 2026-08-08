package com.example.board.dto.boardDTO.response;

public record BoardVoteResultLookupResponse(
        Long voteId,
        long totalVoteCount,
        BoardVoteResultResponse result
) {
}
