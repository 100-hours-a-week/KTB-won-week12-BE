package com.example.board.dto.boardDTO.response;

import com.example.board.domain.user.User;

public record BoardAuthorResponse(Long userId, String nickname, String profileImage) {
    private static final String DELETED_USER_NICKNAME = "삭제된 사용자";

    public static BoardAuthorResponse from(User user, String profileImageUrl) {
        // 탈퇴 회원의 식별 정보와 기존 프로필 이미지를 공개하지 않고 콘텐츠만 유지한다.
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            return new BoardAuthorResponse(null, DELETED_USER_NICKNAME, null);
        }
        return new BoardAuthorResponse(user.getId(), user.getNickname(), profileImageUrl);
    }
}
