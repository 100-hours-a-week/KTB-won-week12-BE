package com.example.board.domain.board;

import com.example.board.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "board_vote_response",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_board_vote_response_vote_user",
                columnNames = {"board_vote_id", "user_id"}
        )
)
public class BoardVoteResponse {
    // 왼쪽 점수만 저장하고 오른쪽 점수는 항상 10에서 뺀 값으로 계산한다.
    public static final int MIN_LEFT_SCORE = 0;
    public static final int MAX_LEFT_SCORE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_vote_response_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_vote_id", nullable = false)
    private BoardVote boardVote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User voter;

    @Column(name = "left_score", nullable = false)
    private int leftScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BoardVoteResponse() {
    }

    private BoardVoteResponse(BoardVote boardVote, User voter, int leftScore, LocalDateTime createdAt) {
        if (boardVote == null || voter == null || createdAt == null) {
            throw new IllegalArgumentException("투표, 사용자, 응답 시각은 필수입니다.");
        }
        validateScore(leftScore);
        this.boardVote = boardVote;
        this.voter = voter;
        this.leftScore = leftScore;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static BoardVoteResponse create(BoardVote boardVote, User voter,
                                           int leftScore, LocalDateTime createdAt) {
        return new BoardVoteResponse(boardVote, voter, leftScore, createdAt);
    }

    public void changeLeftScore(int leftScore, LocalDateTime updatedAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("응답 수정 시각은 필수입니다.");
        }
        validateScore(leftScore);
        // 재투표 시 같은 사용자의 행을 유지하고 선택 점수와 수정 시각만 변경한다.
        this.leftScore = leftScore;
        this.updatedAt = updatedAt;
    }

    public int getRightScore() {
        // 양쪽을 따로 저장하지 않아 합계가 항상 10이라는 불변식을 보장한다.
        return MAX_LEFT_SCORE - leftScore;
    }

    private static void validateScore(int leftScore) {
        if (leftScore < MIN_LEFT_SCORE || leftScore > MAX_LEFT_SCORE) {
            throw new IllegalArgumentException("왼쪽 과실 점수는 0 이상 10 이하여야 합니다.");
        }
    }
}
