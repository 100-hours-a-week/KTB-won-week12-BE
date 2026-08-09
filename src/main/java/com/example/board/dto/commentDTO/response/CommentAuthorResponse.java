package com.example.board.dto.commentDTO.response;

import com.example.board.domain.user.User;

public record CommentAuthorResponse(Long userId, String nickname, String profileImage) {
    private static final String DELETED_USER_NICKNAME = "삭제된 사용자";

    public static CommentAuthorResponse from(User user, String profileImageUrl) {
        // 탈퇴 회원의 댓글은 유지하되 사용자 ID, 닉네임과 프로필 이미지는 익명화한다.
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            return new CommentAuthorResponse(null, DELETED_USER_NICKNAME, null);
        }
        return new CommentAuthorResponse(user.getId(), user.getNickname(), profileImageUrl);
    }
}
