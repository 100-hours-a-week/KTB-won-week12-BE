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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("프로필 이미지만 변경하면 현재 닉네임을 그대로 사용할 수 있다.")
    void currentNicknameCanBeKeptWhenOnlyProfileImageChanges() {
        User user = new User("사과", "apple@naver.com", "encodedPassword", UserRole.USER);
        // 회원정보 수정 테스트는 회원가입 이후 기존 프로필이 등록된 상태를 별도로 구성
        user.changeProfileImageObjectKey("profiles/1/old/original.png");
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");
        String newObjectKey = "profiles/1/11111111-1111-1111-1111-111111111111/original.png";
        UserInfoModifyRequest request = new UserInfoModifyRequest("사과", newObjectKey);

        when(userRepository.existsByNicknameAndIdNotAndIsDeletedFalse("사과", 1L)).thenReturn(false);
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(profileImageStorageService.createDownloadUrl(newObjectKey))
                .thenReturn("https://s3.example/profile");

        UserInfoModifyResponse response = userService.modifyUserInfo(request, principal);

        verify(profileImageStorageService).validateOwnedProfileImage(1L, newObjectKey);
        assertThat(response.getNickname()).isEqualTo("사과");
        assertThat(response.getProfileImage()).isEqualTo("https://s3.example/profile");
    }

    @Test
    @DisplayName("프로필 Object Key를 null로 수정하면 기존 프로필을 제거한다.")
    void nullProfileImageObjectKeyRemovesCurrentProfile() {
        User user = new User("사과", "apple@naver.com", "encodedPassword", UserRole.USER);
        user.changeProfileImageObjectKey("profiles/1/old/original.png");
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");

        when(userRepository.existsByNicknameAndIdNotAndIsDeletedFalse("사과", 1L)).thenReturn(false);
        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));

        UserInfoModifyResponse response = userService.modifyUserInfo(
                new UserInfoModifyRequest("사과", null),
                principal
        );

        // 제거 요청은 조회할 S3 객체가 없으므로 저장소 검증을 건너뛴다.
        verify(profileImageStorageService, never()).validateOwnedProfileImage(any(), any());
        assertThat(user.getProfileImageObjectKey()).isNull();
        assertThat(response.getProfileImage()).isNull();
    }

    @Test
    @DisplayName("내 정보 조회는 DB Object Key 대신 Presigned 프로필 URL을 반환한다.")
    void getUserInfoReturnsProfileImageDownloadUrl() {
        User user = new User("사과", "apple@naver.com", "encodedPassword", UserRole.USER);
        String objectKey = "profiles/1/11111111-1111-1111-1111-111111111111/original.png";
        user.changeProfileImageObjectKey(objectKey);
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(profileImageStorageService.createDownloadUrl(objectKey))
                .thenReturn("https://s3.example/profile");

        var response = userService.getUserInfo(principal);

        assertThat(response.getProfileImage()).isEqualTo("https://s3.example/profile");
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
