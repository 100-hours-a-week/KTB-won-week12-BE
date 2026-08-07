package com.example.board.dto.commentDTO.response;

import com.example.board.domain.user.User;

public record CommentAuthorResponse(Long userId, String nickname, String profileImage) {

    public static CommentAuthorResponse from(User user, String profileImageUrl) {
        return new CommentAuthorResponse(user.getId(), user.getNickname(), profileImageUrl);
    }
}
