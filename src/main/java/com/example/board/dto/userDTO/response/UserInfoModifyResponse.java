package com.example.board.dto.userDTO.response;

import com.example.board.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoModifyResponse {
    private final String nickname;
    // 수정 직후 프론트 상태도 DB에 확정된 Object Key로 갱신할 수 있도록 함께 반환
    private final String profileImageObjectKey;
    private final String profileImage;

    public static UserInfoModifyResponse from(User user, String profileImageUrl){
        return new UserInfoModifyResponse(
                user.getNickname(),
                user.getProfileImageObjectKey(),
                profileImageUrl
        );
    }
}
