package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.board.Board;
import com.example.board.domain.comment.Comment;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.UserRepository;
import com.example.board.service.CommentService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CommentService commentService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User author;
    private User otherUser;
    private Board board;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        author = saveUser("사과", "apple@naver.com");
        otherUser = saveUser("바나나", "banana@naver.com");
        board = boardRepository.saveAndFlush(Board.create(
                author,
                "댓글 테스트 게시글",
                "댓글 테스트 내용",
                List.of()
        ));
    }

    @Test
    @DisplayName("인증된 사용자가 댓글 작성 시 최초 수정 이력을 저장하고 댓글 수를 반영한다.")
    void authenticatedUserCreatesCommentWithInitialHistory() throws Exception {
        mockMvc.perform(post("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "  첫 댓글입니다.  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMENT_CREATED"))
                .andExpect(jsonPath("$.data.commentId").isNumber());

        entityManager.flush();
        entityManager.clear();

        Comment comment = commentRepository.findAll().getFirst();
        assertThat(comment.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(comment.getBoard().getId()).isEqualTo(board.getId());
        assertThat(comment.getCommentModifyRecords()).hasSize(1);
        assertThat(comment.getCommentModifyRecords().getFirst().getContent()).isEqualTo("첫 댓글입니다.");

        mockMvc.perform(get("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentCount").value(1));

        mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].commentCount").value(1));
    }

    @Test
    @DisplayName("댓글 목록을 keyset cursor 방식으로 중복 없이 조회한다.")
    void commentListUsesKeysetCursorWithoutDuplicates() throws Exception {
        Comment first = saveComment("첫 번째 댓글", author);
        saveComment("두 번째 댓글", otherUser);
        Comment third = saveComment("세 번째 댓글", author);

        var firstResult = mockMvc.perform(get("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].commentId").value(third.getId()))
                .andExpect(jsonPath("$.data.content[0].content").value("세 번째 댓글"))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andReturn();

        Number nextCursor = JsonPath.read(
                firstResult.getResponse().getContentAsString(),
                "$.data.nextCursor"
        );

        mockMvc.perform(get("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .param("size", "2")
                        .param("cursor", String.valueOf(nextCursor.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].commentId").value(first.getId()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("미인증 사용자는 댓글 목록을 조회할 수 있고 수정 권한은 false로 반환된다.")
    void anonymousUserCanReadCommentsWithoutEditPermission() throws Exception {
        Comment comment = saveComment("공개 댓글", author);

        mockMvc.perform(get("/boards/{boardId}/comments", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].commentId").value(comment.getId()))
                .andExpect(jsonPath("$.data.content[0].content").value("공개 댓글"))
                .andExpect(jsonPath("$.data.content[0].editableByMe").value(false));
    }

    @Test
    @DisplayName("댓글 목록 조회 시 작성자를 함께 로드하고 쿼리 수를 일정하게 유지한다.")
    void commentListLoadsAuthorsAndUsesFixedQueryCount() {
        saveComment("첫 번째 댓글", author);
        saveComment("두 번째 댓글", otherUser);
        saveComment("세 번째 댓글", author);
        entityManager.flush();
        entityManager.clear();

        Comment loadedComment = commentRepository
                .findCommentSlice(board.getId(), null, PageRequest.of(0, 2))
                .getFirst();
        assertThat(Hibernate.isInitialized(loadedComment.getAuthor())).isTrue();    //Hibernate.isInitialized로 대상 작성자가 Lazy로딩 되지 않고 Entity Graph로 즉시 조회되었는지 확인.

        entityManager.clear();      //조회 후 1차캐시 다시 삭제
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);      //쿼리가 의도한 만큼 전송되었는지 확인
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        try {
            commentService.getComments(
                    board.getId(),
                    null,
                    20,
                    new CustomUserPrincipal(author.getId(), author.getEmail(), "ROLE_USER")
            );
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    @DisplayName("작성자가 댓글을 수정하면 새로운 수정 이력을 추가한다.")
    void authorUpdatesCommentByAppendingHistory() throws Exception {
        Comment comment = saveComment("수정 전 댓글", author);

        mockMvc.perform(patch("/comments/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정 후 댓글"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMENT_UPDATED"))
                .andExpect(jsonPath("$.data.commentId").value(comment.getId()));

        entityManager.flush();
        entityManager.clear();

        Comment updatedComment = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(updatedComment.getCommentModifyRecords()).hasSize(2);
        assertThat(updatedComment.getCommentModifyRecords().getFirst().getContent()).isEqualTo("수정 전 댓글");
        assertThat(updatedComment.getCommentModifyRecords().getLast().getContent()).isEqualTo("수정 후 댓글");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 댓글을 수정하거나 삭제할 수 없다.")
    void nonAuthorCannotUpdateOrDeleteComment() throws Exception {
        Comment comment = saveComment("작성자 댓글", author);

        mockMvc.perform(patch("/comments/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "권한 없는 수정"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("COMMENT_MODIFY_FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("댓글 작성자만 수정하거나 삭제할 수 있습니다."));

        mockMvc.perform(delete("/comments/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser)))
                .andExpect(status().isForbidden());

        assertThat(comment.getCommentModifyRecords()).hasSize(1);
        assertThat(comment.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된 댓글은 목록과 게시글 댓글 수에서 제외되고 다시 변경할 수 없다.")
    void deletedCommentIsExcludedFromListAndBoardCount() throws Exception {
        Comment comment = saveComment("삭제할ㄹ 댓글", author);

        mockMvc.perform(delete("/comments/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getIsDeleted()).isTrue();

        mockMvc.perform(get("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        mockMvc.perform(get("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentCount").value(0));

        mockMvc.perform(patch("/comments/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"삭제 후 수정\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/comments/{commentId}", comment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("삭제된 게시글에는 댓글을 작성하거나 목록을 조회할 수 없다.")
    void deletedBoardRejectsCommentCreationAndList() throws Exception {
        board.deleteBoard();
        boardRepository.saveAndFlush(board);

        mockMvc.perform(post("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "작성할 수 없는 댓글"
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 API는 입력값과 cursor를 검증하고 변경 요청에 인증을 요구한다.")
    void commentApiValidatesInputAndRequiresAuthenticationForChanges() throws Exception {
        Comment comment = saveComment("댓글", author);

        mockMvc.perform(post("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMENT_CONTENT_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("댓글 내용은 필수 입력값입니다."));

        mockMvc.perform(get("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .param("cursor", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/boards/{boardId}/comments", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(author))
                        .param("size", "51"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/boards/{boardId}/comments", board.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/comments/{commentId}", comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/comments/{commentId}", comment.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Comment saveComment(String content, User commentAuthor) {
        return commentRepository.saveAndFlush(Comment.create(commentAuthor, board, content));
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
