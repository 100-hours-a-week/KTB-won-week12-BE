package com.example.board.repository;

import com.example.board.domain.board.BoardLikeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRecordRepository extends JpaRepository<BoardLikeRecord, Long> {
    boolean existsByLikedUserIdAndLikedBoardId(Long userId, Long boardId);
    Optional<BoardLikeRecord> findByLikedUserIdAndLikedBoardId(Long userId, Long boardId);
}
