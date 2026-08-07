package com.example.board.domain.board;

import com.example.board.exception.errorMessage.VoteErrorMessage;
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
        name = "board_vote",
        uniqueConstraints = @UniqueConstraint(name = "uk_board_vote_board", columnNames = "board_id")
)
public class BoardVote {
    // DTO와 도메인이 같은 정책을 사용할 수 있도록 투표 생성 제한을 한곳에 정의한다.
    public static final int MIN_LABEL_LENGTH = 2;
    public static final int MAX_LABEL_LENGTH = 20;
    public static final int MIN_DURATION_HOURS = 1;
    public static final int MAX_DURATION_HOURS = 168;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_vote_id")
    private Long id;

    // Board에 역방향 연관관계를 두지 않아 목록 조회 시 불필요한 존재 확인 쿼리를 방지한다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "left_label", nullable = false, length = MAX_LABEL_LENGTH)
    private String leftLabel;

    @Column(name = "right_label", nullable = false, length = MAX_LABEL_LENGTH)
    private String rightLabel;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    protected BoardVote() {
    }

    private BoardVote(Board board, String leftLabel, String rightLabel,
                      LocalDateTime startedAt, LocalDateTime endsAt) {
        this.board = board;
        this.leftLabel = leftLabel;
        this.rightLabel = rightLabel;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public static BoardVote create(Board board, String leftLabel, String rightLabel,
                                   int durationHours, LocalDateTime startedAt) {
        if (board == null || startedAt == null) {
            throw new IllegalArgumentException("게시글과 투표 시작 시각은 필수입니다.");
        }

        // 공백이 다른 동일 라벨도 중복으로 판단할 수 있도록 먼저 정규화한다.
        String normalizedLeftLabel = normalizeLabel(leftLabel);
        String normalizedRightLabel = normalizeLabel(rightLabel);
        if (normalizedLeftLabel.equals(normalizedRightLabel)) {
            throw new IllegalArgumentException(VoteErrorMessage.LABEL_DUPLICATED);
        }
        if (durationHours < MIN_DURATION_HOURS || durationHours > MAX_DURATION_HOURS) {
            throw new IllegalArgumentException(VoteErrorMessage.DURATION_OUT_OF_RANGE);
        }

        // 종료 시각을 클라이언트에서 받지 않고 서버가 정한 시작 시각과 기간으로 계산한다.
        return new BoardVote(
                board,
                normalizedLeftLabel,
                normalizedRightLabel,
                startedAt,
                startedAt.plusHours(durationHours)
        );
    }

    public boolean isOpen(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("현재 시각은 필수입니다.");
        }
        // 종료 시각과 같아진 순간부터 닫힌 투표로 처리한다.
        return now.isBefore(endsAt);
    }

    public void validateOpen(LocalDateTime now) {
        if (!isOpen(now)) {
            throw new IllegalStateException(VoteErrorMessage.VOTE_CLOSED);
        }
    }

    private static String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(VoteErrorMessage.LABEL_REQUIRED);
        }

        String normalizedLabel = label.trim();
        if (normalizedLabel.length() < MIN_LABEL_LENGTH || normalizedLabel.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException(VoteErrorMessage.LABEL_LENGTH_LIMIT);
        }
        return normalizedLabel;
    }
}
