package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.dto.boardDTO.request.BoardCreateRequest;
import com.example.board.dto.boardDTO.request.BoardUpdateRequest;
import com.example.board.dto.boardDTO.response.BoardCreateResponse;
import com.example.board.dto.boardDTO.response.BoardCursorResponse;
import com.example.board.dto.boardDTO.response.BoardDetailResponse;
import com.example.board.dto.boardDTO.response.BoardUpdateResponse;
import com.example.board.dto.boardDTO.response.BoardLikeResponse;
import com.example.board.response.ApiResponse;
import com.example.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<BoardCursorResponse>> getBoards(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.of("BOARD_LIST", boardService.getBoards(cursor, size))
        );
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.of("BOARD_DETAIL", boardService.getBoard(boardId, principal))
        );
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardUpdateResponse>> updateBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardUpdateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.of("BOARD_UPDATED", boardService.updateBoard(boardId, request, principal))
        );
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        boardService.deleteBoard(boardId, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{boardId}/like")
    public ResponseEntity<ApiResponse<BoardLikeResponse>> likeBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.of("BOARD_LIKE_UPDATED", boardService.likeBoard(boardId, principal))
        );
    }

    @DeleteMapping("/{boardId}/like")
    public ResponseEntity<ApiResponse<BoardLikeResponse>> unlikeBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.of("BOARD_LIKE_UPDATED", boardService.unlikeBoard(boardId, principal))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BoardCreateResponse>> createBoard(
            @Valid @RequestBody BoardCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        BoardCreateResponse response = boardService.createBoard(request, principal);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("BOARD_CREATED", response));
    }
}
