package com.example.board.validation;

import com.example.board.exception.BadRequestException;
import com.example.board.exception.BusinessException;
import com.example.board.exception.ErrorCode;

public final class PasswordValidator {  //비즈니스 요구사항에 따른 비밀번호 검증

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;

    private PasswordValidator() {
        // Utility class
    }

    public static void validate(String password) {
        if (password == null) {
            throw new BadRequestException(ErrorCode.PASSWORD_REQUIRED);
        }

        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BadRequestException(ErrorCode.PASSWORD_LENGTH_LIMIT);
        }

        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char ch : password.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                throw new BadRequestException(ErrorCode.PASSWORD_CANNOT_CONTAIN_BLANK);
            }

            if (Character.isUpperCase(ch)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(ch)) {
                hasLowerCase = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else {
                hasSpecialChar = true;
            }
        }

        if (!hasUpperCase) {
            throw new BadRequestException(ErrorCode.PASSWORD_MUST_CONTAIN_UPPERCASE);
        }

        if (!hasLowerCase) {
            throw new BadRequestException(ErrorCode.PASSWORD_MUST_CONTAIN_LOWERCASE);
        }

        if (!hasDigit) {
            throw new BadRequestException(ErrorCode.PASSWORD_MUST_CONTAIN_NUMBER);
        }

        if (!hasSpecialChar) {
            throw new BadRequestException(ErrorCode.PASSWORD_MUST_CONTAIN_SPECIAL_LETTER);
        }
    }

    public static boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}
