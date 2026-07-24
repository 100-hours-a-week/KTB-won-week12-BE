package com.example.board.dto.authDTO.request;

import com.example.board.exception.errorMessage.UserErrorMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest { //로그인 요청 시 사용
    @NotBlank(message = UserErrorMessage.EMAIL_REQUIRED)
    @Email(message = UserErrorMessage.EMAIL_FORM_INCORRECT)
    private String email;
    private String password;
}
