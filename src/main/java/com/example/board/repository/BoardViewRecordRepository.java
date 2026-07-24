package com.example.board.repository;

import com.example.board.domain.board.BoardViewRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardViewRecordRepository extends JpaRepository<BoardViewRecord, Long> {
    boolean existsByViewedUserIdAndViewedBoardId(Long userId, Long boardId);
}
