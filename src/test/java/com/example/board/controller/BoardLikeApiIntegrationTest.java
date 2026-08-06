package com.example.board.controller;

import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.board.Board;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.repository.BoardLikeRecordRepository;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoardLikeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardLikeRecordRepository boardLikeRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    private User author;
    private User otherUser;
    private Board board;

    @BeforeEach
    void setUp() {
        boardRepository.deleteAll();
        userRepository.deleteAll();

        author = saveUser("사과", "apple@naver.com");
        otherUser = saveUser("바나나", "banana@naver.com");
        board = boardRepository.saveAndFlush(Board.create(
                author,
                "좋아요 테스트 게시글",
                "좋아요 테스트 내용",
                List.of()
        ));
    }

    @Test
    @DisplayName("같은 사용자가 좋아요를 반복 요청해도 한 번만 집계한다.")
    void repeatedLikeRequestIsIdempotent() throws Exception {
        like(board, author)             //perform의 결과를 검증
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_LIKE_UPDATED"))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        like(board, author)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertPersistedLikeState(1);
    }

    @Test
    @DisplayName("같은 사용자가 좋아요 취소를 반복 요청해도 결과가 동일하다.")
    void repeatedUnlikeRequestIsIdempotent() throws Exception {
        like(board, author).andExpect(status().isOk());

        unlike(board, author)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        unlike(board, author)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        assertPersistedLikeState(0);
    }

    @Test
    @DisplayName("사용자별 좋아요를 독립적으로 집계하고 요청자의 좋아요만 취소한다.")
    void differentUsersCreateIndependentLikesAndUnlikeOnlyRemovesRequesterLike() throws Exception {
        like(board, author).andExpect(status().isOk());
        like(board, otherUser)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(2));

        unlike(board, author)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        entityManager.flush();  //수정 사항 DB에 반영
        entityManager.clear();  //1차 캐시 삭제하여 테스트 시 캐싱된 데이터로 검증하지 않도록 한다.

        assertThat(boardLikeRecordRepository.count()).isEqualTo(1);
        assertThat(boardRepository.findById(board.getId()).orElseThrow().getNumberOfLikes()).isEqualTo(1);
        assertThat(boardLikeRecordRepository.existsByLikedUserIdAndLikedBoardId(
                otherUser.getId(), board.getId()
        )).isTrue();
    }

    @Test
    @DisplayName("게시글 상세와 목록에서 저장된 좋아요 상태와 개수를 조회한다.")
    void detailAndListExposePersistedLikeState() throws Exception {
        like(board, author).andExpect(status().isOk());
        like(board, otherUser).andExpect(status().isOk());

        mockMvc.perform(get("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedByMe").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(2));

        mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].likeCount").value(2));
    }

    @Test
    @DisplayName("삭제된 게시글에는 좋아요를 추가하거나 취소할 수 없다.")
    void deletedBoardCannotBeLikedOrUnliked() throws Exception {
        board.deleteBoard();
        boardRepository.saveAndFlush(board);

        like(board, author)
                .andExpect(status().isNotFound());
        unlike(board, author)
                .andExpect(status().isNotFound());

        assertThat(boardLikeRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 좋아요를 추가하거나 취소할 수 없다.")
    void likeAndUnlikeRequireAuthentication() throws Exception {
        mockMvc.perform(put("/boards/{boardId}/like", board.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/boards/{boardId}/like", board.getId()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions like(Board targetBoard, User user) throws Exception {        //반복 사용되는 좋아요 기능을 like로 빼두고 반환값을 사용.
        return mockMvc.perform(put("/boards/{boardId}/like", targetBoard.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user)));
    }

    private ResultActions unlike(Board targetBoard, User user) throws Exception {
        return mockMvc.perform(delete("/boards/{boardId}/like", targetBoard.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerToken(user)));
    }

    private User saveUser(String nickname, String email) {
        return userRepository.saveAndFlush(new User(
                nickname,
                email,
                passwordEncoder.encode("Password12!"),
                UserRole.USER
        ));
    }

    private void assertPersistedLikeState(int expectedCount) {
        Long boardId = board.getId();
        entityManager.flush();
        entityManager.clear();

        assertThat(boardLikeRecordRepository.count()).isEqualTo(expectedCount);
        assertThat(boardRepository.findById(boardId).orElseThrow().getNumberOfLikes())
                .isEqualTo(expectedCount);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                "ROLE_USER"
        );
    }
}
