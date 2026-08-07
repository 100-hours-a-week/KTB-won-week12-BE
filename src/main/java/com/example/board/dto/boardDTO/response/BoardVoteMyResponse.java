package com.example.board.dto.boardDTO.response;

import com.example.board.domain.board.BoardVoteResponse;

public record BoardVoteMyResponse(
        int leftScore,
        int rightScore
) {
    public static BoardVoteMyResponse from(BoardVoteResponse response) {
        return new BoardVoteMyResponse(response.getLeftScore(), response.getRightScore());
    }
}
