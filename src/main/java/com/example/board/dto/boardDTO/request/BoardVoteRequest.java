package com.example.board.dto.boardDTO.request;

import com.example.board.domain.board.BoardVoteResponse;
import com.example.board.exception.errorMessage.VoteErrorMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BoardVoteRequest(
        // 오른쪽 점수는 서버가 10에서 왼쪽 점수를 빼서 계산하므로 요청에는 하나만 받는다.
        @NotNull(message = VoteErrorMessage.SCORE_OUT_OF_RANGE)
        @Min(value = BoardVoteResponse.MIN_LEFT_SCORE, message = VoteErrorMessage.SCORE_OUT_OF_RANGE)
        @Max(value = BoardVoteResponse.MAX_LEFT_SCORE, message = VoteErrorMessage.SCORE_OUT_OF_RANGE)
        Integer leftScore
) {
}
