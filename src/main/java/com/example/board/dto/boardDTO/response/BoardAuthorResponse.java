package com.example.board.dto.boardDTO.response;

import com.example.board.domain.user.User;

public record BoardAuthorResponse(Long userId, String nickname, String profileImage) {

    public static BoardAuthorResponse from(User user) {
        return new BoardAuthorResponse(user.getId(), user.getNickname(), user.getProfileImageObjectKey());
    }
}
