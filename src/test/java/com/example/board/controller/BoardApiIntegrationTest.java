package com.example.board.controller;

import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardModifyRecord;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.BoardViewRecordRepository;
import com.example.board.repository.UserRepository;
import com.example.board.service.BoardService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoardApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardViewRecordRepository boardViewRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private BoardService boardService;

    private User author;

    @BeforeEach
    void setUp() {      //각 테스트 전 레포지토리 초기화 및 작성자 유저 추가.
        boardRepository.deleteAll();
        userRepository.deleteAll();
        author = userRepository.saveAndFlush(new User(
                "사과",
                "apple@naver.com",
                passwordEncoder.encode("Ilikeapple12!"),
                UserRole.USER,
                "https://example.com/profile.png"
        ));
    }

    @Test
    @DisplayName("인증된 사용자가 게시글 작성 시 게시물 수정 이력의 가장 최근 게시물에 추가된다.")
    void authenticatedUserCreatesBoardWithInitialHistoryAndImageUrls() throws Exception {
        mockMvc.perform(post("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "첫 게시글",
                                  "content": "게시글 내용입니다.",
                                  "imageUrls": [
                                    "https://example.com/image-1.png",
                                    "https://example.com/image-2.png"
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BOARD_CREATED"))
                .andExpect(jsonPath("$.data.boardId").isNumber());

        Board board = boardRepository.findAll().getFirst();
        BoardModifyRecord initialHistory = board.getBoardModifyRecords().getFirst();

        assertThat(board.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(initialHistory.getTitle()).isEqualTo("첫 게시글");
        assertThat(initialHistory.getContent()).isEqualTo("게시글 내용입니다.");
        assertThat(initialHistory.getBoardImage())
                .extracting(image -> image.getImageUrl())
                .containsExactly(
                        "https://example.com/image-1.png",
                        "https://example.com/image-2.png"
                );
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 게시글 작성 시 오류가 발생한다.")
    void boardCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "첫 게시글",
                                  "content": "게시글 내용입니다."
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시물 제목과 이미지 경로가 사용 불가하면 게시글이 추가되지 않는다.")
    void boardCreationRejectsInvalidTitleAndImageUrl() throws Exception {
        mockMvc.perform(post("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "스물 여섯 자를 초과하는 게시글 제목은 서버에서 검증되어야합니다",
                                  "content": "게시글 내용입니다.",
                                  "imageUrls": ["not-a-url"]
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(boardRepository.count()).isZero();
    }

    @Test
    @DisplayName("게시글 목록을 keyset cursor 방식으로 중복 없이 조회한다.")
    void boardListUsesKeysetCursorWithoutDuplicates() throws Exception {
        Board first = saveBoard("첫 번째 게시글");
        saveBoard("두 번째 게시글");
        Board third = saveBoard("세 번째 게시글");

        var firstResult = mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_LIST"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].boardId").value(third.getId()))
                .andExpect(jsonPath("$.data.content[0].title").value("세 번째 게시글"))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andReturn();

        Number nextCursor = com.jayway.jsonpath.JsonPath.read(
                firstResult.getResponse().getContentAsString(),
                "$.data.nextCursor"
        );

        mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .param("size", "2")
                        .param("cursor", String.valueOf(nextCursor.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].boardId").value(first.getId()))
                .andExpect(jsonPath("$.data.content[0].title").value("첫 번째 게시글"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("미인증 사용자는 게시글 목록과 상세를 조회할 수 있지만 조회수와 사용자별 상태는 반영되지 않는다.")
    void anonymousUserCanReadBoardsWithoutPersonalStateOrViewCount() throws Exception {
        Board board = saveBoard("공개 게시글");

        mockMvc.perform(get("/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].boardId").value(board.getId()));

        mockMvc.perform(get("/boards/{boardId}", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardId").value(board.getId()))
                .andExpect(jsonPath("$.data.viewCount").value(0))
                .andExpect(jsonPath("$.data.likedByMe").value(false))
                .andExpect(jsonPath("$.data.editableByMe").value(false));

        mockMvc.perform(get("/boards/{boardId}", board.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(0));

        assertThat(boardViewRecordRepository.count()).isZero();
    }

    @Test
    @DisplayName("공개 게시글 조회라도 유효하지 않은 Access Token을 전달하면 인증 오류를 반환한다.")
    void publicBoardReadRejectsInvalidAccessToken() throws Exception {
        saveBoard("공개 게시글");

        mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글 목록 조회 시 EntityGraph로 작성자를 함께 조회한다.")
    void boardListEntityGraphLoadsAuthorWithInitialQuery() {
        saveBoard("게시글");
        entityManager.flush();
        entityManager.clear();

        Board loadedBoard = boardRepository.findBoardSlice(null, PageRequest.of(0, 2)).getFirst();

        assertThat(Hibernate.isInitialized(loadedBoard.getAuthor())).isTrue();
    }

    @Test
    @DisplayName("게시글 수가 증가해도 목록 조회 쿼리 수는 일정하다.")
    void boardListQueryCountDoesNotGrowPerBoard() {
        saveBoard("첫 번째 게시글");
        saveBoard("두 번째 게시글");
        saveBoard("세 번째 게시글");
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);  //Hibernate 전용 구현체인 SessionFactory를 꺼내 테스트에 사용
        Statistics statistics = sessionFactory.getStatistics();                             //Hibernate 실행 통계 관리 객체 사용
        statistics.setStatisticsEnabled(true);                                              //통계 수집 시작 및 초기화
        statistics.clear();

        try {
            boardService.getBoards(null, 10);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);         //게시글과 작성자 조회, 게시물 최신 수정 이력 조회, 게시물 최초 작성 시각 조회, 댓글 조회 총 4번의 쿼리가 발생해야 함
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    @DisplayName("한 사용자의 게시글 조회수는 최초 조회 한 번만 집계한다.")
    void boardDetailCountsOnlyFirstViewPerUser() throws Exception {
        Board board = boardRepository.saveAndFlush(Board.create(
                author,
                "상세 게시글",
                "상세 게시글 내용",
                java.util.List.of("https://example.com/detail.png")
        ));

        mockMvc.perform(get("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_DETAIL"))
                .andExpect(jsonPath("$.data.title").value("상세 게시글"))
                .andExpect(jsonPath("$.data.images[0].imageUrl")
                        .value("https://example.com/detail.png"))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.editableByMe").value(true));

        mockMvc.perform(get("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));      //조회수가 오르지 않았는지 테스트

        assertThat(boardViewRecordRepository.count()).isEqualTo(1);                   //게시물 총 조회수도 함께 테스트
    }

    @Test
    @DisplayName("게시글 목록의 조회 크기가 허용 범위를 벗어나면 요청을 거부한다.")
    void boardListRejectsInvalidCursorSize() throws Exception {
        mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOARD_PAGE_SIZE_INVALID"))
                .andExpect(jsonPath("$.message")
                        .value("게시글 조회 크기는 1 이상 50 이하여야 합니다."));
    }

    @Test
    @DisplayName("작성자가 게시글을 수정하면 새로운 수정 이력을 추가한다.")
    void authorUpdatesBoardByAppendingNewHistory() throws Exception {
        Board board = boardRepository.saveAndFlush(Board.create(
                author,
                "수정 전 제목",
                "수정 전 내용",
                java.util.List.of("https://example.com/before.png")
        ));

        mockMvc.perform(patch("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정 후 제목",
                                  "content": "수정 후 내용",
                                  "imageUrls": ["https://example.com/after.png"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_UPDATED"))
                .andExpect(jsonPath("$.data.boardId").value(board.getId()));

        entityManager.flush();
        entityManager.clear();

        Board updatedBoard = boardRepository.findById(board.getId()).orElseThrow();
        assertThat(updatedBoard.getBoardModifyRecords()).hasSize(2);
        assertThat(updatedBoard.getBoardModifyRecords().getFirst().getTitle()).isEqualTo("수정 전 제목");
        assertThat(updatedBoard.getBoardModifyRecords().getLast().getTitle()).isEqualTo("수정 후 제목");
        assertThat(updatedBoard.getBoardModifyRecords().getLast().getBoardImage())
                .extracting(image -> image.getImageUrl())
                .containsExactly("https://example.com/after.png");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 게시글을 수정하거나 삭제할 수 없다.")
    void nonAuthorCannotUpdateOrDeleteBoard() throws Exception {
        Board board = saveBoard("작성자 게시글");
        User otherUser = userRepository.saveAndFlush(new User(
                "바나나",
                "banana@naver.com",
                passwordEncoder.encode("Ilikebanana12!"),
                UserRole.USER,
                "https://example.com/banana.png"
        ));

        mockMvc.perform(patch("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "권한 없는 수정",
                                  "content": "수정할 수 없습니다."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("BOARD_MODIFY_FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("게시글 작성자만 수정하거나 삭제할 수 있습니다."));

        mockMvc.perform(delete("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otherUser)))
                .andExpect(status().isForbidden());

        assertThat(board.getBoardModifyRecords()).hasSize(1);
        assertThat(board.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("작성자가 게시글을 삭제하면 소프트 삭제되고 조회 결과에서 제외된다.")
    void authorSoftDeletesBoardAndDeletedBoardIsNotExposed() throws Exception {
        Board board = saveBoard("삭제할 게시글");

        mockMvc.perform(delete("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(boardRepository.findById(board.getId()).orElseThrow().isDeleted()).isTrue();

        mockMvc.perform(get("/boards/{boardId}", board.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("게시글이 존재하지 않습니다."));

        mockMvc.perform(get("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    private Board saveBoard(String title) {
        return boardRepository.saveAndFlush(Board.create(
                author,
                title,
                title + " 내용",
                java.util.List.of()
        ));
    }

    private String bearerToken() {
        return bearerToken(author);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                "ROLE_USER"
        );
    }
}
