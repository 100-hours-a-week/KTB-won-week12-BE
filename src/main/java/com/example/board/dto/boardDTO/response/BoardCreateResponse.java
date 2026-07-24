package com.example.board.dto.boardDTO.response;

import com.example.board.domain.board.Board;

public record BoardCreateResponse(Long boardId) {

    public static BoardCreateResponse from(Board board) {
        return new BoardCreateResponse(board.getId());
    }
}
