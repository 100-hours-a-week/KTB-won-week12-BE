package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.domain.user.User;
import com.example.board.dto.userDTO.request.UserInfoModifyRequest;
import com.example.board.dto.userDTO.response.UserInfoModifyResponse;
import com.example.board.dto.userDTO.response.UserInfoResponse;
import com.example.board.exception.ConflictException;
import com.example.board.exception.ErrorCode;
import com.example.board.exception.NotFoundException;
import com.example.board.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    public UserInfoResponse getUserInfo(CustomUserPrincipal principal){
        Long userId = principal.getUserId();

        Optional<User> optionalUserFoundById = userRepository.findByIdAndIsDeletedFalse(userId);
        User userFoundById = optionalUserFoundById.orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND)
        );

        return UserInfoResponse.from(userFoundById);
    }

    @Transactional
    public UserInfoModifyResponse modifyUserInfo(UserInfoModifyRequest userInfoModifyRequest, CustomUserPrincipal principal){

        Long userId = principal.getUserId();

        String newNickname = userInfoModifyRequest.getNickname();
        String newProfileImage = userInfoModifyRequest.getProfileImage();

        if(userRepository.existsByNicknameAndIdNotAndIsDeletedFalse(newNickname, userId)){   //다른 사용자의 닉네임과 중복 체크
            throw new ConflictException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        Optional<User> optionalModifyTargetUser = userRepository.findByIdAndIsDeletedFalse(userId);
        User modifyTargetUser = optionalModifyTargetUser.orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND)
        );

        modifyTargetUser.changeNickname(newNickname);
        modifyTargetUser.changeProfileImage(newProfileImage);   //더티체킹으로 update쿼리 자동 전송

        return UserInfoModifyResponse.from(modifyTargetUser);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndIsDeletedFalse(email);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNicknameAndIsDeletedFalse(nickname);
    }
}
