package com.example.board.repository;

import com.example.board.domain.board.BoardVote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {

    @Query("""
            select vote
            from BoardVote vote
            where vote.board.id = :boardId
              and vote.board.isDeleted = false
            """)
    Optional<BoardVote> findByActiveBoardId(@Param("boardId") Long boardId);

    // 같은 투표에 대한 최초 응답과 재투표를 직렬화하여 중복 생성 경쟁을 방지한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select vote
            from BoardVote vote
            where vote.board.id = :boardId
              and vote.board.isDeleted = false
            """)
    Optional<BoardVote> findByActiveBoardIdWithWriteLock(@Param("boardId") Long boardId);
}
