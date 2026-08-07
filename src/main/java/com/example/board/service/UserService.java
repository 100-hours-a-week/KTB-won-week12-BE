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
    private final ProfileImageStorageService profileImageStorageService;
    public UserInfoResponse getUserInfo(CustomUserPrincipal principal){
        Long userId = principal.getUserId();

        Optional<User> optionalUserFoundById = userRepository.findByIdAndIsDeletedFalse(userId);
        User userFoundById = optionalUserFoundById.orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND)
        );

        return UserInfoResponse.from(
                userFoundById,
                profileImageStorageService.createDownloadUrl(userFoundById.getProfileImageObjectKey())
        );
    }

    @Transactional
    public UserInfoModifyResponse modifyUserInfo(UserInfoModifyRequest userInfoModifyRequest, CustomUserPrincipal principal){

        Long userId = principal.getUserId();

        String newNickname = userInfoModifyRequest.getNickname();
        String newProfileImageObjectKey = userInfoModifyRequest.getProfileImageObjectKey();

        if(userRepository.existsByNicknameAndIdNotAndIsDeletedFalse(newNickname, userId)){   //다른 사용자의 닉네임과 중복 체크
            throw new ConflictException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        Optional<User> optionalModifyTargetUser = userRepository.findByIdAndIsDeletedFalse(userId);
        User modifyTargetUser = optionalModifyTargetUser.orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND)
        );

        if (newProfileImageObjectKey != null) {
            // DB에 저장하기 전에 현재 사용자 소유 경로와 실제 S3 객체 메타데이터를 모두 확인한다.
            profileImageStorageService.validateOwnedProfileImage(userId, newProfileImageObjectKey);
        }

        modifyTargetUser.changeNickname(newNickname);
        // null은 프로필 제거를 의미하며, 값이 있으면 검증된 Object Key로 교체한다.
        modifyTargetUser.changeProfileImageObjectKey(newProfileImageObjectKey);

        return UserInfoModifyResponse.from(
                modifyTargetUser,
                profileImageStorageService.createDownloadUrl(modifyTargetUser.getProfileImageObjectKey())
        );
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndIsDeletedFalse(email);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNicknameAndIsDeletedFalse(nickname);
    }
}
