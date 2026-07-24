package com.example.board.service;

import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
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
class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailService customUserDetailService;

    @Test
    @DisplayName("사용자 조회 시 DB에 저장된 역할만 권한으로 부여한다.")
    void userReceivesOnlyItsPersistedRole() {
        User user = new User("사과", "apple@naver.com", "encodedPassword", UserRole.USER, "image");
        when(userRepository.findByEmailAndIsDeletedFalse(user.getEmail())).thenReturn(Optional.of(user));

        var userDetails = customUserDetailService.loadUserByUsername(user.getEmail());

        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }
}
