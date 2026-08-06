package com.example.board.controller;

import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest         //통합 테스트를 위한 Bean 등록
@AutoConfigureMockMvc   //MockMvc 구성
@Transactional          //전송된 쿼리를 롤백.
class AuthUserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;      //Bean에 등록된 요소 삽입

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); //기존 정보 있으면 삭제
        user = userRepository.saveAndFlush(new User(        //각 테스트 수행 전 해당 유저 객체 삽입 후 저장.
                "사과",
                "apple@naver.com",
                passwordEncoder.encode("Ilikeapple12!"),
                UserRole.USER
        ));
    }

    @Test
    @DisplayName("로그인 시 Access Token을 반환하고 HttpOnly로 Refresh Token을 반환한다.")
    void loginReturnsAccessTokenAndHttpOnlyRefreshTokenCookie() throws Exception {
        mockMvc.perform(post("/auth/login")             //해당 API로 가짜 post 요청을 보낸 경우
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "apple@naver.com",
                                  "password": "Ilikeapple12!"
                                }
                                """))       //ObjectMapper 사용 가능하지만 이렇게 작성하는 것이 더 직관적.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_LOGIN"))  //응답 JSON에서 code를 추출
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())          //응답 JSON에서 data의 하위에 accessToken이 정상적으로 반환되는지 확안
                .andExpect(cookie().httpOnly("refreshToken", true));        //응답의 쿠키에 refreshToken이 있는지 확인
    }

    @Test
    @DisplayName("로그인 정보가 올바르지 않으면 안정적인 오류 코드와 메시지를 반환한다.")
    void loginFailureReturnsErrorCodeAndMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "apple@naver.com",
                                  "password": "WrongPassword12!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("이메일 또는 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("잘못된 JSON 요청은 400 오류 코드와 메시지를 반환한다.")
    void malformedJsonReturnsErrorCodeAndMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message")
                        .value("요청 본문의 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("중복된 이메일로 가입하면 충돌 오류 코드와 메시지를 반환한다.")
    void duplicateEmailReturnsErrorCodeAndMessage() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "apple@naver.com",
                                  "password": "NewPassword12!",
                                  "nickname": "새사과"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message")
                        .value("이미 존재하는 이메일입니다."));
    }

    @Test
    @DisplayName("회원가입 사용자는 프로필 이미지 Object Key 없이 생성된다.")
    void signupCreatesUserWithoutProfileImageObjectKey() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new-user@naver.com",
                                  "password": "NewPassword12!",
                                  "nickname": "새사용자"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("USER_SIGNUP"))
                .andExpect(jsonPath("$.data.email").value("new-user@naver.com"))
                .andExpect(jsonPath("$.data.nickname").value("새사용자"));

        // 프로필 이미지는 로그인 후 회원정보 수정 API에서만 등록한다.
        User signedUpUser = userRepository.findByEmailAndIsDeletedFalse("new-user@naver.com")
                .orElseThrow();
        assertThat(signedUpUser.getProfileImage()).isNull();
    }

    @Test
    @DisplayName("이메일과 닉네임 체크는 권한 설정 없이 가능하다.")
    void availabilityEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/users/email-availability")
                        .param("email", "apple@naver.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));

        mockMvc.perform(get("/users/nickname-availability")
                        .param("nickname", "새닉네임"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("내 정보 조회 시 인가가 필요하고, CustomPrincial에 등록된 정보를 사용한다.")
    void myInfoRequiresAuthenticationAndUsesJwtPrincipal() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("인증에 실패했습니다"));

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("apple@naver.com"))
                .andExpect(jsonPath("$.data.nickname").value("사과"));
    }

    @Test
    @DisplayName("계정 삭제 시 만료된 RefreshToken을 반환하고, 사용자는 소프트 삭제한다.")
    void deleteMyAccountSoftDeletesUserAndExpiresRefreshToken() throws Exception {
        mockMvc.perform(delete("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deleteReason": "더 이상 사용하지 않음"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USER_DELETED"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getIsDeleted()).isTrue();
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                "ROLE_USER"
        );
    }
}
