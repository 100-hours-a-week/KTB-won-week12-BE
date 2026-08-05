package com.example.board.controller;

import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.dto.uploadDTO.request.BoardImageUploadUrlRequest;
import com.example.board.dto.uploadDTO.response.BoardImageUploadUrlResponse;
import com.example.board.dto.uploadDTO.response.BoardImageUploadUrlsResponse;
import com.example.board.repository.UserRepository;
import com.example.board.service.BoardImageStorageService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UploadApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BoardImageStorageService boardImageStorageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(new User(
                "사과",
                "upload@naver.com",
                passwordEncoder.encode("Ilikeapple12!"),
                UserRole.USER,
                null
        ));
    }

    @Test
    @DisplayName("인증된 사용자는 게시글 원본과 썸네일의 Presigned PUT URL을 발급받는다.")
    void authenticatedUserCreatesBoardImageUploadUrls() throws Exception {
        when(boardImageStorageService.createUploadUrls(
                eq(user.getId()),
                any(BoardImageUploadUrlRequest.class)
        )).thenReturn(new BoardImageUploadUrlsResponse(List.of(
                new BoardImageUploadUrlResponse(
                        "boards/1/group/original.png",
                        "https://s3.example/original",
                        "image/png",
                        "boards/1/group/thumbnail.webp",
                        "https://s3.example/thumbnail",
                        "image/webp",
                        Instant.parse("2026-08-05T10:05:00Z")
                )
        )));

        mockMvc.perform(post("/uploads/board-images/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "images": [
                                    {
                                      "originalFileName": "accident.png",
                                      "originalContentType": "image/png",
                                      "originalSize": 1048576,
                                      "thumbnailContentType": "image/webp",
                                      "thumbnailSize": 102400
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOARD_IMAGE_UPLOAD_URLS_CREATED"))
                .andExpect(jsonPath("$.data.images[0].originalUploadUrl")
                        .value("https://s3.example/original"))
                .andExpect(jsonPath("$.data.images[0].thumbnailUploadUrl")
                        .value("https://s3.example/thumbnail"));
    }

    @Test
    @DisplayName("미인증 사용자는 게시글 이미지 Presigned URL을 발급받을 수 없다.")
    void uploadUrlCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/uploads/board-images/presigned-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "images": []
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                "ROLE_USER"
        );
    }
}
