package com.example.board.domain.board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BoardVoteTest {
    // 도메인 규칙만 검증하므로 영속화가 필요 없는 연관 엔티티는 Mock으로 대체한다.
    private final Board board = mock(Board.class);
    // 경계 시각을 정확히 비교하기 위해 테스트에서는 고정된 서버 시각을 사용한다.
    private final LocalDateTime startedAt = LocalDateTime.of(2026, 8, 7, 12, 0);

    @Test
    @DisplayName("투표 대상의 공백을 제거하고 서버 시작 시각에 기간을 더해 종료 시각을 정한다.")
    void createsVoteWithNormalizedLabelsAndServerTime() {
        BoardVote vote = BoardVote.create(board, "  A 차량 ", " B 차량  ", 24, startedAt);

        assertThat(vote.getLeftLabel()).isEqualTo("A 차량");
        assertThat(vote.getRightLabel()).isEqualTo("B 차량");
        assertThat(vote.getStartedAt()).isEqualTo(startedAt);
        assertThat(vote.getEndsAt()).isEqualTo(startedAt.plusHours(24));
    }

    @Test
    @DisplayName("투표 기간의 최솟값과 최댓값을 허용한다.")
    void acceptsDurationBoundaries() {
        assertThat(BoardVote.create(board, "A 차량", "B 차량", 1, startedAt).getEndsAt())
                .isEqualTo(startedAt.plusHours(1));
        assertThat(BoardVote.create(board, "A 차량", "B 차량", 168, startedAt).getEndsAt())
                .isEqualTo(startedAt.plusHours(168));
    }

    @Test
    @DisplayName("범위를 벗어난 기간과 동일한 투표 대상을 거부한다.")
    void rejectsInvalidDurationAndDuplicatedLabels() {
        // 기간 하한과 trim 이후의 라벨 중복을 각각 검증한다.
        assertThatThrownBy(() -> BoardVote.create(board, "A 차량", "B 차량", 0, startedAt))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoardVote.create(board, " A 차량 ", "A 차량", 24, startedAt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("종료 시각 전까지만 투표를 열린 상태로 판단한다.")
    void determinesOpenStateUsingCurrentServerTime() {
        BoardVote vote = BoardVote.create(board, "A 차량", "B 차량", 1, startedAt);

        // 종료 직전은 OPEN이지만 종료 시각부터는 CLOSED로 판단해야 한다.
        assertThat(vote.isOpen(startedAt.plusMinutes(59))).isTrue();
        assertThat(vote.getStatus(startedAt.plusMinutes(59))).isEqualTo(BoardVoteStatus.OPEN);
        assertThat(vote.isOpen(startedAt.plusHours(1))).isFalse();
        assertThat(vote.getStatus(startedAt.plusHours(1))).isEqualTo(BoardVoteStatus.CLOSED);
        assertThatThrownBy(() -> vote.validateOpen(startedAt.plusHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
