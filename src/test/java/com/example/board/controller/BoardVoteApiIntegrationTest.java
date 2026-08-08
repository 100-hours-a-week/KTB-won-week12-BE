package com.example.board.controller;

import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardVote;
import com.example.board.domain.board.BoardVoteResponse;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.BoardVoteRepository;
import com.example.board.repository.BoardVoteResponseRepository;
import com.example.board.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoardVoteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardVoteRepository boardVoteRepository;

    @Autowired
    private BoardVoteResponseRepository responseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    private User author;
    private Board board;

    @BeforeEach
    void setUp() {
        responseRepository.deleteAll();
        boardVoteRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        author = saveUser("작성자", "vote-author@example.com");
        board = boardRepository.saveAndFlush(Board.create(
                author,
                "과실 투표 게시글",
                "사고 상황 설명",
                List.of()
        ));
    }

    @Test
    @DisplayName("인증 사용자의 첫 투표를 저장하고 현재 집계와 내 응답을 반환한다.")
    void createsFirstVoteResponse() throws Exception {
        BoardVote vote = saveOpenVote();

        vote(author, 3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_VOTE_UPDATED"))
                .andExpect(jsonPath("$.data.voteId").value(vote.getId()))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.totalVoteCount").value(1))
                .andExpect(jsonPath("$.data.result.leftScore").value(3))
                .andExpect(jsonPath("$.data.result.rightScore").value(7))
                .andExpect(jsonPath("$.data.myVote.leftScore").value(3))
                .andExpect(jsonPath("$.data.myVote.rightScore").value(7));

        assertThat(responseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("재투표는 응답 행을 추가하지 않고 기존 점수만 변경한다.")
    void updatesExistingVoteResponse() throws Exception {
        BoardVote vote = saveOpenVote();
        vote(author, 2).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        Long responseId = findResponse(vote, author).getId();

        vote(author, 8)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVoteCount").value(1))
                .andExpect(jsonPath("$.data.myVote.leftScore").value(8))
                .andExpect(jsonPath("$.data.myVote.rightScore").value(2));

        entityManager.flush();
        entityManager.clear();
        assertThat(responseRepository.count()).isEqualTo(1);
        assertThat(findResponse(vote, author).getId()).isEqualTo(responseId);
    }

    @Test
    @DisplayName("같은 점수의 재요청은 응답과 수정 시각을 변경하지 않는다.")
    void sameScoreRequestIsIdempotent() throws Exception {
        BoardVote vote = saveOpenVote();
        vote(author, 6).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        LocalDateTime firstUpdatedAt = findResponse(vote, author).getUpdatedAt();

        vote(author, 6).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertThat(responseRepository.count()).isEqualTo(1);
        assertThat(findResponse(vote, author).getUpdatedAt()).isEqualTo(firstUpdatedAt);
    }

    @Test
    @DisplayName("여러 사용자 응답의 평균은 HALF_UP으로 반올림하고 오른쪽 결과는 10에서 뺀다.")
    void aggregatesResponsesAfterVoting() throws Exception {
        saveOpenVote();
        User otherUser = saveUser("다른투표자", "other-voter@example.com");
        vote(author, 4).andExpect(status().isOk());

        vote(otherUser, 5)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVoteCount").value(2))
                .andExpect(jsonPath("$.data.result.leftScore").value(5))
                .andExpect(jsonPath("$.data.result.rightScore").value(5));
    }

    @Test
    @DisplayName("인증되지 않았거나 범위를 벗어난 점수로는 투표할 수 없다.")
    void rejectsUnauthenticatedAndOutOfRangeVote() throws Exception {
        saveOpenVote();

        mockMvc.perform(put("/boards/{boardId}/vote", board.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leftScore\": 5}"))
                .andExpect(status().isUnauthorized());

        vote(author, 11)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VOTE_SCORE_OUT_OF_RANGE"));

        assertThat(responseRepository.count()).isZero();
    }

    @Test
    @DisplayName("종료된 투표에는 최초 투표와 재투표를 할 수 없다.")
    void rejectsClosedVote() throws Exception {
        boardVoteRepository.saveAndFlush(BoardVote.create(
                board,
                "A 차량",
                "B 차량",
                1,
                LocalDateTime.now().minusHours(2)
        ));

        vote(author, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOARD_VOTE_CLOSED"));
    }

    @Test
    @DisplayName("투표가 없는 게시글과 삭제된 게시글을 서로 다른 오류로 구분한다.")
    void distinguishesMissingVoteAndDeletedBoard() throws Exception {
        vote(author, 5)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_VOTE_NOT_FOUND"));

        saveOpenVote();
        board.deleteBoard();
        boardRepository.saveAndFlush(board);

        vote(author, 5)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    @DisplayName("미인증 사용자도 응답이 없는 투표 결과를 0건과 null로 조회한다.")
    void anonymousUserGetsEmptyVoteResult() throws Exception {
        BoardVote vote = saveOpenVote();

        mockMvc.perform(get("/boards/{boardId}/vote/result", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_VOTE_RESULT"))
                .andExpect(jsonPath("$.data.voteId").value(vote.getId()))
                .andExpect(jsonPath("$.data.totalVoteCount").value(0))
                .andExpect(jsonPath("$.data.result").doesNotExist());
    }

    @Test
    @DisplayName("미인증 사용자도 명시적으로 요청하면 현재 집계 결과를 조회한다.")
    void anonymousUserGetsAggregateResult() throws Exception {
        saveOpenVote();
        User otherUser = saveUser("결과투표자", "result-voter@example.com");
        vote(author, 4).andExpect(status().isOk());
        vote(otherUser, 5).andExpect(status().isOk());

        mockMvc.perform(get("/boards/{boardId}/vote/result", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVoteCount").value(2))
                .andExpect(jsonPath("$.data.result.leftScore").value(5))
                .andExpect(jsonPath("$.data.result.rightScore").value(5));
    }

    @Test
    @DisplayName("종료된 투표 결과는 조회할 수 있지만 투표가 없거나 게시글이 삭제되면 조회할 수 없다.")
    void getsClosedResultAndRejectsMissingVoteOrDeletedBoard() throws Exception {
        mockMvc.perform(get("/boards/{boardId}/vote/result", board.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_VOTE_NOT_FOUND"));

        BoardVote closedVote = boardVoteRepository.saveAndFlush(BoardVote.create(
                board,
                "A 차량",
                "B 차량",
                1,
                LocalDateTime.now().minusHours(2)
        ));
        responseRepository.saveAndFlush(BoardVoteResponse.create(
                closedVote,
                author,
                7,
                LocalDateTime.now().minusHours(2)
        ));

        mockMvc.perform(get("/boards/{boardId}/vote/result", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.leftScore").value(7));

        board.deleteBoard();
        boardRepository.saveAndFlush(board);
        mockMvc.perform(get("/boards/{boardId}/vote/result", board.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"));
    }

    private BoardVote saveOpenVote() {
        return boardVoteRepository.saveAndFlush(BoardVote.create(
                board,
                "A 차량",
                "B 차량",
                24,
                LocalDateTime.now().minusMinutes(1)
        ));
    }

    private BoardVoteResponse findResponse(BoardVote vote, User voter) {
        return responseRepository.findByBoardVoteIdAndVoterId(vote.getId(), voter.getId())
                .orElseThrow();
    }

    private ResultActions vote(User voter, int leftScore) throws Exception {
        return mockMvc.perform(put("/boards/{boardId}/vote", board.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(voter))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"leftScore\": %d}".formatted(leftScore)));
    }

    private User saveUser(String nickname, String email) {
        return userRepository.saveAndFlush(new User(
                nickname,
                email,
                passwordEncoder.encode("Password12!"),
                UserRole.USER
        ));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                "ROLE_USER"
        );
    }
}
