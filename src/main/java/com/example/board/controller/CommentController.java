package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.dto.commentDTO.request.CommentCreateRequest;
import com.example.board.dto.commentDTO.request.CommentUpdateRequest;
import com.example.board.dto.commentDTO.response.CommentCreateResponse;
import com.example.board.dto.commentDTO.response.CommentCursorResponse;
import com.example.board.dto.commentDTO.response.CommentUpdateResponse;
import com.example.board.response.ApiResponse;
import com.example.board.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/boards/{boardId}/comments")
    public ResponseEntity<ApiResponse<CommentCursorResponse>> getComments(
            @PathVariable Long boardId,
            @RequestParam(required = false) Long cursor,    //무한 스크롤 시 사용할 마지막 조회 댓글 Id
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.of("COMMENT_LIST", commentService.getComments(boardId, cursor, size, principal))
        );
    }

    @PostMapping("/boards/{boardId}/comments")
    public ResponseEntity<ApiResponse<CommentCreateResponse>> createComment(
            @PathVariable Long boardId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        "COMMENT_CREATED",
                        commentService.createComment(boardId, request, principal)
                ));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentUpdateResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.of("COMMENT_UPDATED", commentService.updateComment(commentId, request, principal))
        );
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        commentService.deleteComment(commentId, principal);
        return ResponseEntity.noContent().build();
    }
}
