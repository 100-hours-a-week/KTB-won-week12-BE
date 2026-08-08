package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardVote;
import com.example.board.domain.board.BoardVoteResponse;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.dto.boardDTO.request.BoardVoteRequest;
import com.example.board.dto.boardDTO.response.BoardVoteUpdateResponse;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.BoardVoteRepository;
import com.example.board.repository.BoardVoteResponseRepository;
import com.example.board.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoardVoteConcurrencyIntegrationTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardVoteRepository boardVoteRepository;

    @Autowired
    private BoardVoteResponseRepository responseRepository;

    @Autowired
    private UserRepository userRepository;

    private User voter;
    private Board board;
    private BoardVote vote;

    @BeforeEach
    void setUp() {
        clearVoteData();

        voter = userRepository.saveAndFlush(new User(
                "동시투표자",
                "concurrent-voter@example.com",
                "encoded-password",
                UserRole.USER
        ));
        board = boardRepository.saveAndFlush(Board.create(
                voter,
                "동시 투표 게시글",
                "동시 요청 검증",
                List.of()
        ));
        vote = boardVoteRepository.saveAndFlush(BoardVote.create(
                board,
                "A 차량",
                "B 차량",
                24,
                LocalDateTime.now().minusMinutes(1)
        ));
    }

    @AfterEach
    void tearDown() {
        clearVoteData();
    }

    @RepeatedTest(3)
    @DisplayName("같은 사용자의 동시 최초 투표는 한 응답 행으로 직렬화된다.")
    void concurrentFirstVotesCreateOnlyOneResponse() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CustomUserPrincipal principal = new CustomUserPrincipal(
                voter.getId(),
                voter.getEmail(),
                "ROLE_USER"
        );

        try {
            Future<BoardVoteUpdateResponse> leftTwo = executor.submit(
                    () -> voteAfterSignal(principal, 2, ready, start)
            );
            Future<BoardVoteUpdateResponse> leftEight = executor.submit(
                    () -> voteAfterSignal(principal, 8, ready, start)
            );

            // 두 작업 스레드가 준비된 뒤 동시에 서비스 트랜잭션을 시작시킨다.
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            BoardVoteUpdateResponse firstResult = leftTwo.get(10, TimeUnit.SECONDS);
            BoardVoteUpdateResponse secondResult = leftEight.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.myVote().leftScore()).isEqualTo(2);
            assertThat(secondResult.myVote().leftScore()).isEqualTo(8);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        List<BoardVoteResponse> persistedResponses = responseRepository.findAll();

        // 비관적 락이 두 find-none/insert 흐름을 직렬화하여 UNIQUE 충돌 없이 한 행만 남긴다.
        assertThat(persistedResponses).hasSize(1);
        assertThat(persistedResponses.getFirst().getBoardVote().getId()).isEqualTo(vote.getId());
        assertThat(persistedResponses.getFirst().getVoter().getId()).isEqualTo(voter.getId());
        assertThat(persistedResponses.getFirst().getLeftScore()).isIn(2, 8);
    }

    private BoardVoteUpdateResponse voteAfterSignal(
            CustomUserPrincipal principal,
            int leftScore,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 투표 시작 신호를 받지 못했습니다.");
        }
        return boardService.vote(board.getId(), new BoardVoteRequest(leftScore), principal);
    }

    private void clearVoteData() {
        responseRepository.deleteAll();
        boardVoteRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();
    }
}
