package com.example.board.dto.userDTO.response;

import com.example.board.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoModifyResponse {
    private final String nickname;
    private final String profileImage;

    public static UserInfoModifyResponse from(User user, String profileImageUrl){
        return new UserInfoModifyResponse(user.getNickname(), profileImageUrl);
    }
}
