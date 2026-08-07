package com.example.board.repository;

import com.example.board.domain.board.BoardVoteResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardVoteResponseRepository extends JpaRepository<BoardVoteResponse, Long> {

    Optional<BoardVoteResponse> findByBoardVoteIdAndVoterId(Long boardVoteId, Long voterId);

    @Query("""
            select count(response.id) as totalVoteCount,
                   avg(response.leftScore) as averageLeftScore
            from BoardVoteResponse response
            where response.boardVote.id = :boardVoteId
            """)
    BoardVoteAggregateProjection findAggregateByBoardVoteId(
            @Param("boardVoteId") Long boardVoteId
    );
}
