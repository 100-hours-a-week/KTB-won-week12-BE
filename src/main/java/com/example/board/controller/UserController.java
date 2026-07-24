package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.configuration.jwt.RefreshTokenCookieFactory;
import com.example.board.dto.authDTO.response.DeleteResponse;
import com.example.board.dto.userDTO.request.UserDeleteRequest;
import com.example.board.dto.userDTO.request.UserInfoModifyRequest;
import com.example.board.dto.userDTO.request.UserPasswordChangeRequest;
import com.example.board.dto.userDTO.response.AvailabilityResponse;
import com.example.board.dto.userDTO.response.UserInfoModifyResponse;
import com.example.board.dto.userDTO.response.UserInfoResponse;
import com.example.board.response.ApiResponse;
import com.example.board.service.AuthService;
import com.example.board.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @GetMapping("/email-availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkEmailAvailability(@RequestParam String email) {
        return ResponseEntity.ok(
                ApiResponse.of("EMAIL_AVAILABILITY", new AvailabilityResponse(userService.isEmailAvailable(email)))
        );
    }

    @GetMapping("/nickname-availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkNicknameAvailability(@RequestParam String nickname) {
        return ResponseEntity.ok(
                ApiResponse.of("NICKNAME_AVAILABILITY", new AvailabilityResponse(userService.isNicknameAvailable(nickname)))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(@AuthenticationPrincipal CustomUserPrincipal principal){
        UserInfoResponse response = userService.getUserInfo(principal);

        return ResponseEntity.ok(
                ApiResponse.of("USER_INFO", response)
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoModifyResponse>> updateUserInfo(
            @Valid @RequestBody UserInfoModifyRequest userInfoModifyRequest,
            @AuthenticationPrincipal CustomUserPrincipal principal){
        UserInfoModifyResponse response = userService.modifyUserInfo(userInfoModifyRequest, principal);

        return ResponseEntity.ok(
                ApiResponse.of("USER_UPDATE", response)
        );
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updateUserPassword(
            @Valid @RequestBody UserPasswordChangeRequest userPasswordChangeRequest,
            @AuthenticationPrincipal CustomUserPrincipal principal){
        authService.changeUserPassword(userPasswordChangeRequest, principal);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteUser(
            @RequestBody UserDeleteRequest userDeleteRequest,
            HttpServletResponse response,
            @AuthenticationPrincipal CustomUserPrincipal principal){
        authService.deleteUser(userDeleteRequest, principal);

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expire().toString());

        return ResponseEntity.ok(
                ApiResponse.of("USER_DELETED", new DeleteResponse(""))
        );
    }
}
