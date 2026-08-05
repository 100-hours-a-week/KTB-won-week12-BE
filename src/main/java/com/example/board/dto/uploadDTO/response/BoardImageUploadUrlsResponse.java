package com.example.board.dto.uploadDTO.response;

import java.util.List;

public record BoardImageUploadUrlsResponse(
        List<BoardImageUploadUrlResponse> images
) {
}
