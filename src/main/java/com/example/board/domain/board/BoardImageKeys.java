package com.example.board.domain.board;

/**
 * 게시글 이미지의 원본과 썸네일 S3 Object Key를 항상 한 쌍으로 전달한다.
 */
public record BoardImageKeys(
        String originalObjectKey,
        String thumbnailObjectKey
) {
}
