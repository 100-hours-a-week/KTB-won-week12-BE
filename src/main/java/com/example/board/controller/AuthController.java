package com.example.board.controller;

import com.example.board.configuration.jwt.RefreshTokenCookieFactory;
import com.example.board.configuration.jwt.TokenResult;
import com.example.board.dto.authDTO.request.LoginRequest;
import com.example.board.dto.authDTO.response.*;
import com.example.board.dto.authDTO.request.SignupRequest;
import com.example.board.response.ApiResponse;
import com.example.board.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(CsrfToken csrfToken){ //프론트엔드로 CSRF 토큰 전달
        return csrfToken;
    }

    @PostMapping("/signup") //회원가입 메소드
    public ResponseEntity<ApiResponse<SignupResponse>> userSignup(@Valid @RequestBody SignupRequest signupRequest){
        SignupResponse response = authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("USER_SIGNUP", response));
    }

    @PostMapping("/login")  //로그인 메소드
    public ResponseEntity<ApiResponse<LoginResponse>> userLogin( @Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response){
        TokenResult tokenResult = authService.login(loginRequest);

        ResponseCookie refreshTokenCookie = refreshTokenCookieFactory.create(tokenResult.refreshToken());

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
        );

        return ResponseEntity.ok()        //헤더에는 refreshToken, 바디에는 accessToken 넣어 전송.
                .body(ApiResponse.of("USER_LOGIN", new LoginResponse(
                        tokenResult.accessToken()
                                )
                        )
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken){
        String accessToken = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok()
                .body(ApiResponse.of("ACCESS_TOKEN_REFRESH", new RefreshResponse(accessToken)));
    }

    @PostMapping("/logout") //로그아웃
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletResponse response){
        String accessToken = "";

        expireRefreshToken(response);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.of("USER_LOGOUT", new LogoutResponse(
                        accessToken
                        )
                    )
                );
    }

    private void expireRefreshToken(HttpServletResponse response){
        ResponseCookie expiredRefreshToken = refreshTokenCookieFactory.expire();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                expiredRefreshToken.toString()
        );
    }
}
