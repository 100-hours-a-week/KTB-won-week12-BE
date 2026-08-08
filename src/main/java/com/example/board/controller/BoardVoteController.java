package com.example.board.controller;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.dto.boardDTO.request.BoardVoteRequest;
import com.example.board.dto.boardDTO.response.BoardVoteUpdateResponse;
import com.example.board.response.ApiResponse;
import com.example.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards/{boardId}/vote")
public class BoardVoteController {
    private final BoardService boardService;

    @PutMapping
    public ResponseEntity<ApiResponse<BoardVoteUpdateResponse>> vote(
            @PathVariable Long boardId,
            @Valid @RequestBody BoardVoteRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                ApiResponse.of("BOARD_VOTE_UPDATED", boardService.vote(boardId, request, principal))
        );
    }
}
