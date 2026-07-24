package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.dto.userDTO.request.UserInfoModifyRequest;
import com.example.board.dto.userDTO.response.UserInfoModifyResponse;
import com.example.board.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("프로필 이미지만 변경하면 현재 닉네임을 그대로 사용할 수 있다.")
    void currentNicknameCanBeKeptWhenOnlyProfileImageChanges() {
        User user = new User("사과", "apple@naver.com", "encodedPassword", UserRole.USER, "old-image");
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");
        UserInfoModifyRequest request = new UserInfoModifyRequest("사과", "new-image");

        when(userRepository.existsByNicknameAndIdNotAndIsDeletedFalse("사과", 1L)).thenReturn(false);
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));

        UserInfoModifyResponse response = userService.modifyUserInfo(request, principal);

        assertThat(response.getNickname()).isEqualTo("사과");
        assertThat(response.getProfileImage()).isEqualTo("new-image");
    }

    @Test
    @DisplayName("사용 가능한 이메일과 닉네임은 활성 사용자의 존재 여부와 반대로 반환된다.")
    void availabilityIsInverseOfActiveUserExistence() {
        when(userRepository.existsByEmailAndIsDeletedFalse("apple@naver.com")).thenReturn(true);
        when(userRepository.existsByNicknameAndIsDeletedFalse("새닉네임")).thenReturn(false);

        assertThat(userService.isEmailAvailable("apple@naver.com")).isFalse();
        assertThat(userService.isNicknameAvailable("새닉네임")).isTrue();
    }
}
