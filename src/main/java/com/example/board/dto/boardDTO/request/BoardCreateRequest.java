package com.example.board.dto.boardDTO.request;

import com.example.board.domain.board.BoardImageKeys;
import com.example.board.exception.errorMessage.BoardErrorMessage;
import com.example.board.exception.errorMessage.ImageErrorMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BoardCreateRequest(
        @NotBlank(message = BoardErrorMessage.TITLE_REQUIRED)
        @Size(max = 26, message = BoardErrorMessage.TITLE_LENGTH_LIMIT)
        String title,

        @NotBlank(message = BoardErrorMessage.CONTENT_REQUIRED)
        String content,

        @Size(max = 5, message = ImageErrorMessage.IMAGE_COUNT_LIMIT)
        List<@NotNull @Valid BoardImageKeyRequest> images,

        // vote 자체는 선택값이지만 전달된 경우에는 내부 라벨과 기간까지 검증한다.
        @Valid
        BoardVoteCreateRequest vote
) {
    public List<BoardImageKeys> safeImages() {
        return images == null ? List.of() : images.stream().map(BoardImageKeyRequest::toDomain).toList();
    }
}
