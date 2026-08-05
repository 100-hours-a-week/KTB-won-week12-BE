package com.example.board.repository;

import com.example.board.domain.board.BoardModifyRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoardModifyRecordRepository extends JpaRepository<BoardModifyRecord, Long> {

    @EntityGraph(attributePaths = "boardImage")
    @Query("""
            select record
            from BoardModifyRecord record
            where record.board.id in :boardIds
              and record.id = (
                  select max(latest.id)
                  from BoardModifyRecord latest
                  where latest.board.id = record.board.id
              )
            """)
    List<BoardModifyRecord> findLatestRecordsByBoardIds(@Param("boardIds") Collection<Long> boardIds);

    @Query("""
            select record.board.id as boardId, min(record.registDate) as createdAt
            from BoardModifyRecord record
            where record.board.id in :boardIds
            group by record.board.id
            """)
    List<BoardCreatedAtProjection> findCreatedAtByBoardIds(@Param("boardIds") Collection<Long> boardIds);

    Optional<BoardModifyRecord> findFirstByBoardIdOrderByIdAsc(Long boardId);

    @EntityGraph(attributePaths = "boardImage")
    Optional<BoardModifyRecord> findFirstByBoardIdOrderByIdDesc(Long boardId);
}
