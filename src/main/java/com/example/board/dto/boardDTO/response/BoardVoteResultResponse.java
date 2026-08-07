package com.example.board.dto.boardDTO.response;

public record BoardVoteResultResponse(
        int leftScore,
        int rightScore
) {
    public static BoardVoteResultResponse fromLeftScore(int leftScore) {
        // 오른쪽 결과를 별도로 반올림하지 않아 두 결과의 합이 항상 10이 되도록 한다.
        return new BoardVoteResultResponse(leftScore, 10 - leftScore);
    }
}
