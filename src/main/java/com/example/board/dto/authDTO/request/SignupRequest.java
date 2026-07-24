package com.example.board.dto.authDTO.request;

import com.example.board.exception.errorMessage.UserErrorMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequest {
    @NotBlank(message = UserErrorMessage.EMAIL_REQUIRED)
    @Email(message = UserErrorMessage.EMAIL_FORM_INCORRECT)
    private String email;
    private String password;
    @NotBlank(message = UserErrorMessage.NICKNAME_REQUIRED)
    @Size(min = 2, max = 10, message = UserErrorMessage.NICKNAME_LENGTH_LIMIT)
    private String nickname;
    private String profileImage;
}
