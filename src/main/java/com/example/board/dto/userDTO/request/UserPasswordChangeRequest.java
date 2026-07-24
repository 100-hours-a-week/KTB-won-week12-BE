package com.example.board.dto.userDTO.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPasswordChangeRequest {
    //비밀번호 검증은 Service 계층에서 PasswordValidator를 사용해 검증
    private String originalPassword;
    private String changedPassword;
}
