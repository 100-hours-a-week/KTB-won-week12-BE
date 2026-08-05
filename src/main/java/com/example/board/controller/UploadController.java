package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.dto.uploadDTO.request.BoardImageUploadUrlRequest;
import com.example.board.dto.uploadDTO.response.BoardImageUploadUrlsResponse;
import com.example.board.response.ApiResponse;
import com.example.board.service.BoardImageStorageService;
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
}
