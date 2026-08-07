package com.example.board.domain.board;

import com.example.board.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BoardVoteResponseTest {
    // 응답의 점수 계산만 검증하므로 연관 엔티티는 Mock으로 대체한다.
    private final BoardVote boardVote = mock(BoardVote.class);
    private final User voter = mock(User.class);
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 8, 7, 13, 0);

    @Test
    @DisplayName("0:10, 5:5, 10:0 과실 비율을 모두 저장할 수 있다.")
    void acceptsAllScoreBoundaries() {
        BoardVoteResponse leftZero = BoardVoteResponse.create(boardVote, voter, 0, createdAt);
        BoardVoteResponse middle = BoardVoteResponse.create(boardVote, voter, 5, createdAt);
        BoardVoteResponse leftTen = BoardVoteResponse.create(boardVote, voter, 10, createdAt);

        assertThat(leftZero.getRightScore()).isEqualTo(10);
        assertThat(middle.getRightScore()).isEqualTo(5);
        assertThat(leftTen.getRightScore()).isZero();
    }

    @Test
    @DisplayName("응답을 변경하면 점수와 수정 시각만 갱신한다.")
    void changesExistingResponse() {
        BoardVoteResponse response = BoardVoteResponse.create(boardVote, voter, 3, createdAt);
        LocalDateTime updatedAt = createdAt.plusMinutes(10);

        // 재투표 후에도 최초 생성 시각은 유지되고 수정 시각만 바뀌어야 한다.
        response.changeLeftScore(7, updatedAt);

        assertThat(response.getLeftScore()).isEqualTo(7);
        assertThat(response.getRightScore()).isEqualTo(3);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("0부터 10까지의 범위를 벗어난 점수는 거부한다.")
    void rejectsOutOfRangeScore() {
        assertThatThrownBy(() -> BoardVoteResponse.create(boardVote, voter, -1, createdAt))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoardVoteResponse.create(boardVote, voter, 11, createdAt))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
