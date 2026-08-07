package com.example.board.dto.boardDTO.request;

import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardVote;
import com.example.board.exception.errorMessage.VoteErrorMessage;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BoardVoteCreateRequest(
        @NotBlank(message = VoteErrorMessage.LABEL_REQUIRED)
        String leftLabel,

        @NotBlank(message = VoteErrorMessage.LABEL_REQUIRED)
        String rightLabel,

        @NotNull(message = VoteErrorMessage.DURATION_OUT_OF_RANGE)
        @Min(value = BoardVote.MIN_DURATION_HOURS, message = VoteErrorMessage.DURATION_OUT_OF_RANGE)
        @Max(value = BoardVote.MAX_DURATION_HOURS, message = VoteErrorMessage.DURATION_OUT_OF_RANGE)
        Integer durationHours
) {
    // @Size는 trim 이전 길이를 검사하므로 정규화된 라벨 길이는 별도 교차 검증한다.
    @AssertTrue(message = VoteErrorMessage.LABEL_LENGTH_LIMIT)
    public boolean hasValidNormalizedLabelLengths() {
        return hasValidLength(leftLabel) && hasValidLength(rightLabel);
    }

    // 양쪽 값의 앞뒤 공백을 제거한 뒤 같다면 동일한 투표 대상으로 판단한다.
    @AssertTrue(message = VoteErrorMessage.LABEL_DUPLICATED)
    public boolean hasDifferentLabels() {
        if (leftLabel == null || rightLabel == null || leftLabel.isBlank() || rightLabel.isBlank()) {
            return true; // 필수값 오류는 각 필드의 @NotBlank가 처리한다.
        }
        return !leftLabel.trim().equals(rightLabel.trim());
    }

    public BoardVote toEntity(Board board, LocalDateTime startedAt) {
        return BoardVote.create(board, leftLabel, rightLabel, durationHours, startedAt);
    }

    private boolean hasValidLength(String label) {
        if (label == null || label.isBlank()) {
            return true; // 필수값 오류와 길이 오류가 중복으로 생성되는 것을 방지한다.
        }
        int normalizedLength = label.trim().length();
        return normalizedLength >= BoardVote.MIN_LABEL_LENGTH
                && normalizedLength <= BoardVote.MAX_LABEL_LENGTH;
    }
}
