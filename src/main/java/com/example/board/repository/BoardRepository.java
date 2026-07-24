package com.example.board.repository;

import com.example.board.domain.board.Board;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    boolean existsByIdAndIsDeletedFalse(Long boardId);

    @EntityGraph(attributePaths = "author") //N+1문제를 막기 위해 EntityGraph를 사용하여 작성자를 함께 조회.
    @Query("""
            select board
            from Board board
            where board.isDeleted = false
              and (:cursor is null or board.id < :cursor)
            order by board.id desc
            """)    //cursor보다 작은 게시글 번호를 가진 요소 조회.
    List<Board> findBoardSlice(@Param("cursor") Long cursor, Pageable pageable);    //Keyset Cursor와 Pageable을 이용한 페이지네이션 구현

    @EntityGraph(attributePaths = "author")
    @Query("""
            select board
            from Board board
            where board.id = :boardId
              and board.isDeleted = false
            """)
    Optional<Board> findActiveBoard(@Param("boardId") Long boardId);

    @EntityGraph(attributePaths = "author")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select board
            from Board board
            where board.id = :boardId
              and board.isDeleted = false
            """)
    Optional<Board> findActiveBoardWithWriteLock(@Param("boardId") Long boardId);
}
