package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.dto.uploadDTO.request.BoardImageUploadUrlRequest;
import com.example.board.dto.uploadDTO.request.ProfileImageUploadUrlRequest;
import com.example.board.dto.uploadDTO.response.BoardImageUploadUrlsResponse;
import com.example.board.dto.uploadDTO.response.ProfileImageUploadUrlResponse;
import com.example.board.response.ApiResponse;
import com.example.board.service.BoardImageStorageService;
import com.example.board.service.ProfileImageStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/uploads")
public class UploadController {

    private final BoardImageStorageService boardImageStorageService;
    private final ProfileImageStorageService profileImageStorageService;

    @PostMapping("/board-images/presigned-urls")
    public ResponseEntity<ApiResponse<BoardImageUploadUrlsResponse>> createBoardImageUploadUrls(
            @Valid @RequestBody BoardImageUploadUrlRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "BOARD_IMAGE_UPLOAD_URLS_CREATED",
                boardImageStorageService.createUploadUrls(principal.getUserId(), request)
        ));
    }

    @PostMapping("/profile-image/presigned-url")
    public ResponseEntity<ApiResponse<ProfileImageUploadUrlResponse>> createProfileImageUploadUrl(
            @Valid @RequestBody ProfileImageUploadUrlRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        // Object Key에 인증 사용자 ID를 포함해 다른 사용자의 프로필 경로를 발급받지 못하게 한다.
        return ResponseEntity.ok(ApiResponse.of(
                "PROFILE_IMAGE_UPLOAD_URL_CREATED",
                profileImageStorageService.createUploadUrl(principal.getUserId(), request)
        ));
    }
}
