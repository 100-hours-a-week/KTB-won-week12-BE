package com.example.board.dto.userDTO.response;

import com.example.board.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoResponse {
    private final String email;
    private final String nickname;
    private final String profileImage;

    public static UserInfoResponse from(User user){
        return new UserInfoResponse(user.getEmail(),
                user.getNickname(),
                user.getProfileImage());
    }
}
