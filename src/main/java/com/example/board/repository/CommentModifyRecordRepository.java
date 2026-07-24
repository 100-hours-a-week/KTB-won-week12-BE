package com.example.board.repository;

import com.example.board.domain.comment.CommentModifyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentModifyRecordRepository extends JpaRepository<CommentModifyRecord, Long> {

    @Query("""
            select record
            from CommentModifyRecord record
            where record.comment.id in :commentIds
              and record.id = (
                  select max(latest.id)
                  from CommentModifyRecord latest
                  where latest.comment.id = record.comment.id
              )
            """)
    List<CommentModifyRecord> findLatestRecordsByCommentIds(
            @Param("commentIds") Collection<Long> commentIds
    );

    @Query("""
            select record.comment.id as commentId, min(record.registDate) as createdAt
            from CommentModifyRecord record
            where record.comment.id in :commentIds
            group by record.comment.id
            """)
    List<CommentCreatedAtProjection> findCreatedAtByCommentIds(
            @Param("commentIds") Collection<Long> commentIds
    );
}
