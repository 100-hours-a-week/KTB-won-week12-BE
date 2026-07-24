package com.example.board.repository;

import com.example.board.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select comment.board.id as boardId, count(comment.id) as commentCount
            from Comment comment
            where comment.board.id in :boardIds
              and comment.isDeleted = false
            group by comment.board.id
            """)
    List<BoardCommentCountProjection> countActiveCommentsByBoardIds(
            @Param("boardIds") Collection<Long> boardIds
    );

    long countByBoardIdAndIsDeletedFalse(Long boardId);

    @EntityGraph(attributePaths = "author") //N+1 문제를 막기 위한 Entity Graph 사용
    @Query("""
            select comment
            from Comment comment
            where comment.board.id = :boardId
              and comment.isDeleted = false
              and comment.parentComment is null
              and (:cursor is null or comment.id < :cursor)
            order by comment.id desc
            """)
    List<Comment> findCommentSlice(
            @Param("boardId") Long boardId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "author") //N+1 문제를 막기 위한 Entity Graph 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)   //게시글 수정,  삭제 시 비관적 락을 사용한 조회
    @Query("""
            select comment
            from Comment comment
            where comment.id = :commentId
              and comment.isDeleted = false
            """)
    Optional<Comment> findActiveCommentWithWriteLock(@Param("commentId") Long commentId);
}
