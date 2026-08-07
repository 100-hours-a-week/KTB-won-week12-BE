package com.example.board.exception.errorMessage;

public final class VoteErrorMessage {
    public static final String SCORE_OUT_OF_RANGE = "과실 점수는 0 이상 10 이하여야 합니다.";
    public static final String LABEL_REQUIRED = "투표 대상은 필수 입력값입니다.";
    public static final String LABEL_LENGTH_LIMIT = "투표 대상은 2자 이상 20자 이하여야 합니다.";
    public static final String LABEL_DUPLICATED = "양쪽 투표 대상은 서로 달라야 합니다.";
    public static final String DURATION_OUT_OF_RANGE = "투표 기간은 1시간 이상 168시간 이하여야 합니다.";
    public static final String VOTE_NOT_FOUND = "게시글에 투표가 존재하지 않습니다.";
    public static final String VOTE_CLOSED = "종료된 투표입니다.";

    private VoteErrorMessage() {
    }
}
