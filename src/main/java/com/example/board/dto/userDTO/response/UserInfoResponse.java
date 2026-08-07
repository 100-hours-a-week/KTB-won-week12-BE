package com.example.board.dto.userDTO.response;

import com.example.board.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoResponse {
    private final String email;
    private final String nickname;
    // 본인 화면에서 기존 프로필 유지·교체·삭제 상태를 판단할 영구 식별자
    private final String profileImageObjectKey;
    private final String profileImage;

    public static UserInfoResponse from(User user, String profileImageUrl){
        return new UserInfoResponse(user.getEmail(),
                user.getNickname(),
                user.getProfileImageObjectKey(),
                profileImageUrl);
    }
}
