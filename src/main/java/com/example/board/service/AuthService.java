package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.configuration.jwt.JwtTokenProvider;
import com.example.board.configuration.jwt.TokenResult;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import com.example.board.dto.authDTO.request.LoginRequest;
import com.example.board.dto.authDTO.request.SignupRequest;
import com.example.board.dto.authDTO.response.SignupResponse;
import com.example.board.dto.userDTO.request.UserDeleteRequest;
import com.example.board.dto.userDTO.request.UserPasswordChangeRequest;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.ConflictException;
import com.example.board.exception.ErrorCode;
import com.example.board.exception.NotFoundException;
import com.example.board.exception.UnauthorizedException;
import com.example.board.repository.UserRepository;
import com.example.board.validation.PasswordValidator;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResult login(@Valid LoginRequest loginRequest){

        PasswordValidator.validate(loginRequest.getPassword()); //비밀번호 형식 검증

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

        authenticationManager.authenticate(authenticationToken);

        User userFindByEmail = userRepository.findByEmailAndIsDeletedFalse(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.LOGIN_FAILED));

        String authority = "ROLE_" + userFindByEmail.getUserRole().name();  //ROLE_ 형태로 맞추기 위해 문자열 추가

        String accessToken = jwtTokenProvider.createAccessToken(
                userFindByEmail.getId(),
                userFindByEmail.getEmail(),
                authority);
        String refreshToken = jwtTokenProvider.createRefreshToken(userFindByEmail.getId());             //AccessToken과 RefreshToken 생성

        return new TokenResult(accessToken, refreshToken);
    }
    @Transactional
    public SignupResponse signup(@Valid SignupRequest signupRequest){
        String email = signupRequest.getEmail();
        String password = signupRequest.getPassword();
        String nickname = signupRequest.getNickname();

        checkEmailDuplication(email);
        checkNicknameDuplication(nickname);  //이메일과 닉네임 중복 체크 후 중복된다면 예외 발생
        PasswordValidator.validate(password);

        password = passwordEncoder.encode(password);    //Bean에 등록된 암호화 모듈 사용하여 암호화된 값을 저장

        // 프로필 이미지는 인증 후 회원정보 수정에서만 등록하므로 신규 사용자는 Object Key 없이 생성
        User user = new User(nickname, email, password, UserRole.USER);

        userRepository.save(user);

        return SignupResponse.from(user);
    }

    public String refreshAccessToken(String refreshToken){      //refreshToken을 이용한 accessToken 재발급
        if(refreshToken == null){
            throw new UnauthorizedException(ErrorCode.USER_UNAUTHENTICATED);
        }

        Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);

        Long userId = jwtTokenProvider.getUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        if(user.getIsDeleted()){
            throw new UnauthorizedException(ErrorCode.USER_DELETED);
        }

        String authority = "ROLE_" + user.getUserRole();

        return jwtTokenProvider.createAccessToken(
                userId,
                user.getEmail(),
                authority);
    }

    @Transactional
    public void deleteUser(UserDeleteRequest userDeleteRequest, CustomUserPrincipal principal){

        Long userId = principal.getUserId();

        User deleteTargetUser = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND)
        );

        String deleteReason = userDeleteRequest.getDeleteReason();

        deleteTargetUser.deleteUser(deleteReason);
    }

    @Transactional
    public void changeUserPassword(UserPasswordChangeRequest userPasswordChangeRequest, CustomUserPrincipal principal){

        Long userId = principal.getUserId();

        User userFindById = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        String currentPassword = userPasswordChangeRequest.getOriginalPassword();
        String changePassword = userPasswordChangeRequest.getChangedPassword();

        String encodedCurrentPassword = userFindById.getPassword();
        if(!passwordEncoder.matches(currentPassword, encodedCurrentPassword)){
            throw new BadRequestException(ErrorCode.PASSWORD_INCORRECT);     //비밀번호 변경 시 비밀번호 일치 여부 확인 로직 추가
        }

        PasswordValidator.validate(changePassword);

        if(passwordEncoder.matches(changePassword, encodedCurrentPassword)){
            throw new BadRequestException(ErrorCode.PASSWORD_ALREADY_USED);  //현재 비밀번호로 변경 시도 시 에러 발생
        }

        changePassword = passwordEncoder.encode(changePassword);

        userFindById.changeEncodedPassword(changePassword);
    }


    public void checkEmailDuplication(String email){        //이메일 중복체크 => 중복 시 예외처리
        if(userRepository.existsByEmailAndIsDeletedFalse(email)){
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        };
    }

    public void checkNicknameDuplication(String nickname){  //닉네임 중복체크 => 중복 시 예외처리
        if(userRepository.existsByNicknameAndIsDeletedFalse(nickname)){
            throw new ConflictException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }
}
