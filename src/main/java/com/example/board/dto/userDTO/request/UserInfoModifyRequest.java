package com.example.board.dto.userDTO.request;

import com.example.board.exception.errorMessage.UserErrorMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoModifyRequest {
    @NotBlank(message = UserErrorMessage.NICKNAME_REQUIRED)
    @Size(min = 2, max = 10, message = UserErrorMessage.NICKNAME_LENGTH_LIMIT)
    private String nickname;
    private String profileImage;
}
