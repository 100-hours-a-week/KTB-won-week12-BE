package com.example.board.dto.boardDTO.response;

import com.example.board.domain.board.BoardImage;

public record BoardImageResponse(Long imageId, String imageUrl) {

    public static BoardImageResponse from(BoardImage image) {
        return new BoardImageResponse(image.getId(), image.getImageUrl());
    }
}
