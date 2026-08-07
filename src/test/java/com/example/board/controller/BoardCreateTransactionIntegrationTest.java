package com.example.board.controller;

import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.board.BoardVote;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.BoardVoteRepository;
import com.example.board.repository.UserRepository;
import com.example.board.service.BoardImageStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardCreateTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BoardVoteRepository boardVoteRepository;

    @MockitoBean
    private BoardImageStorageService boardImageStorageService;

    private User author;

    @BeforeEach
    void setUp() {
        boardRepository.deleteAll();
        userRepository.deleteAll();
        author = userRepository.saveAndFlush(new User(
                "작성자",
                "transaction@example.com",
                passwordEncoder.encode("Password12!"),
                UserRole.USER
        ));
    }

    @AfterEach
    void tearDown() {
        boardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("투표 저장에 실패하면 같은 트랜잭션에서 생성한 게시글도 롤백한다.")
    void rollsBackBoardWhenVoteCreationFails() throws Exception {
        when(boardVoteRepository.save(any(BoardVote.class)))
                .thenThrow(new IllegalStateException("투표 저장 실패"));

        mockMvc.perform(post("/boards")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "롤백 대상 게시글",
                                  "content": "투표 저장 실패 상황",
                                  "images": [],
                                  "vote": {
                                    "leftLabel": "A 차량",
                                    "rightLabel": "B 차량",
                                    "durationHours": 24
                                  }
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));

        // 게시글 INSERT 이후 투표 저장에서 예외가 발생해도 트랜잭션 전체가 취소되어야 한다.
        assertThat(boardRepository.count()).isZero();
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                author.getId(),
                author.getEmail(),
                "ROLE_USER"
        );
    }
}
