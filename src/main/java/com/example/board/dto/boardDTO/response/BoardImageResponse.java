package com.example.board.dto.boardDTO.response;

import com.example.board.domain.board.BoardImage;

public record BoardImageResponse(
        Long imageId,
        String originalObjectKey,
        String thumbnailObjectKey,
        String originalImageUrl,
        String thumbnailImageUrl
) {

    public static BoardImageResponse from(
            BoardImage image,
            String originalImageUrl,
            String thumbnailImageUrl
    ) {
        return new BoardImageResponse(
                image.getId(),
                image.getOriginalObjectKey(),
                image.getThumbnailObjectKey(),
                originalImageUrl,
                thumbnailImageUrl
        );
    }
}
