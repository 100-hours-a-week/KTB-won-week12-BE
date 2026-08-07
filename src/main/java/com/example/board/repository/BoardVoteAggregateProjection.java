package com.example.board.repository;

public interface BoardVoteAggregateProjection {
    // 응답 엔티티 전체를 메모리에 올리지 않고 DB가 계산한 집계값만 받는다.
    long getTotalVoteCount();
    Double getAverageLeftScore();
}
