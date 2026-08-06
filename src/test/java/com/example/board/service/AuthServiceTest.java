package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.configuration.jwt.TokenResult;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.dto.authDTO.request.LoginRequest;
import com.example.board.dto.authDTO.request.SignupRequest;
import com.example.board.dto.userDTO.request.UserDeleteRequest;
import com.example.board.dto.userDTO.request.UserPasswordChangeRequest;
import com.example.board.exception.ConflictException;
import com.example.board.exception.BadRequestException;
import com.example.board.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    UserRepository userRepository;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("로그인에 성공하면 Access Token과 Refresh Token을 발급한다.")
    void loginSuccessTest() {
        //given
        User testUser = new User("사과", "apple@naver.com", "Ilikeapple12!", UserRole.USER);

        LoginRequest request = new LoginRequest(testUser.getEmail(), testUser.getPassword());
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(testUser.getEmail(), testUser.getPassword());
        Authentication authentication = mock(Authentication.class);

        //when
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmailAndIsDeletedFalse(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.createAccessToken(any(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refreshToken");
        TokenResult tokenResult = new TokenResult("accessToken", "refreshToken");
        //then
        assertThat(authService.login(request)).isEqualTo(tokenResult);
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로 회원가입할 수 없다.")
    void emailDuplicationTest(){
        //given
        String email = "apple@naver.com";

        //when
        when(userRepository.existsByEmailAndIsDeletedFalse("apple@naver.com")).thenReturn(true);

        //then
        assertThrows(ConflictException.class, () -> authService.checkEmailDuplication(email));
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 회원가입할 수 없다.")
    void nicknameDuplicationTest(){
        //given
        String nickname = "사과사과";

        //when
        when(userRepository.existsByNicknameAndIsDeletedFalse("사과사과")).thenReturn(true);

        //then
        assertThrows(ConflictException.class, () -> authService.checkNicknameDuplication(nickname));
    }

    @Test
    @DisplayName("유효한 회원 정보로 회원가입하면 암호화된 비밀번호와 USER 역할을 저장한다.")
    void signupSuccessTest() {
        //given
        SignupRequest signupRequest = new SignupRequest("apple@naver.com", "Ilikeapple12!", "사과사과");
        User newUser = new User(signupRequest.getNickname(), signupRequest.getEmail(), signupRequest.getPassword(), UserRole.USER);
        //when
        when(userRepository.existsByNicknameAndIsDeletedFalse("사과사과")).thenReturn(false);
        when(userRepository.existsByEmailAndIsDeletedFalse("apple@naver.com")).thenReturn(false);

        //then
        assertThat(authService.signup(signupRequest).getEmail()).isEqualTo(newUser.getEmail());
        assertThat(authService.signup(signupRequest).getNickname()).isEqualTo(newUser.getNickname());
        // 회원가입에서는 프로필 이미지를 입력받지 않으므로 저장 대상 사용자도 Object Key가 없어야 함
        verify(userRepository, times(2)).save(argThat(user -> user.getProfileImage() == null));
    }

    @Test
    @DisplayName("계정 삭제 요청 시 사용자를 소프트 삭제한다.")
    void deleteUser() {
        // given
        UserDeleteRequest userDeleteRequest = new UserDeleteRequest("탈퇴 사유");
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");

        User deleteTargetUser = mock(User.class);

        when(userRepository.findByIdAndIsDeletedFalse(principal.getUserId()))
                .thenReturn(Optional.of(deleteTargetUser));

        // when
        authService.deleteUser(userDeleteRequest, principal);

        // then
        verify(userRepository, times(1)).findByIdAndIsDeletedFalse(principal.getUserId());
        verify(deleteTargetUser, times(1)).deleteUser("탈퇴 사유");
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하면 새로운 비밀번호로 변경한다.")
    void passwordChangeTest(){
        //given
        User targetUser = new User("사과", "apple@naver.com", "Ilikeapple12!", UserRole.USER);
        UserPasswordChangeRequest userPasswordChangeRequest = new UserPasswordChangeRequest("Ilikeapple12!", "Ilikecherry12!");
        CustomUserPrincipal userPrincipal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");
        //when
        when(userRepository.findByIdAndIsDeletedFalse(any())).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("Ilikeapple12!", "Ilikeapple12!")).thenReturn(true);
        when(passwordEncoder.matches("Ilikecherry12!", "Ilikeapple12!")).thenReturn(false);
        when(passwordEncoder.encode("Ilikecherry12!")).thenReturn("encodedChangedPassword");
        authService.changeUserPassword(userPasswordChangeRequest, userPrincipal);
        //then
        assertThat(targetUser.getPassword()).isEqualTo("encodedChangedPassword");
    }

    @Test
    @DisplayName("새 비밀번호가 현재 비밀번호와 같으면 변경할 수 없다.")
    void passwordChangeRejectsCurrentlyUsedPassword(){
        User targetUser = new User("사과", "apple@naver.com", "encodedCurrentPassword", UserRole.USER);
        UserPasswordChangeRequest request = new UserPasswordChangeRequest("Ilikeapple12!", "Ilikeapple12!");
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "apple@naver.com", "ROLE_USER");

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.matches("Ilikeapple12!", "encodedCurrentPassword")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.changeUserPassword(request, principal));
        verify(passwordEncoder, never()).encode(anyString());
    }

}
