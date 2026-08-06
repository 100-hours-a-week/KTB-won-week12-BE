package com.example.board.dto.userDTO.request;

import com.example.board.exception.errorMessage.UserErrorMessage;
import com.example.board.exception.errorMessage.ImageErrorMessage;
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
    // null이면 기존 프로필 제거, 값이 있으면 업로드가 완료된 S3 Object Key로 교체한다.
    @Size(max = 512, message = ImageErrorMessage.OBJECT_KEY_LENGTH_LIMIT)
    private String profileImageObjectKey;
}
