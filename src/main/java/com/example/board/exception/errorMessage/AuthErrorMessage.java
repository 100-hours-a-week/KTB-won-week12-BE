package com.example.board.exception.errorMessage;

public class AuthErrorMessage {
    public static final String LOGIN_FAILED = "이메일 또는 비밀번호가 올바르지 않습니다.";
    public static final String USER_UNAUTHENTICATED = "인증 정보가 존재하지 않습니다.";
    public static final String AUTHENTICATION_FAILED = "인증에 실패했습니다";
    public static final String USER_UNAUTHORIZED = "인가되지 않은 접근입니다.";
    public static final String USER_DELETED = "탈퇴한 사용자입니다.";

    //비밀번호 에러메세지
    public static final String PASSWORD_REQUIRED = "비밀번호는 필수 입력값입니다.";
    public static final String PASSWORD_LENGTH_LIMIT = "비밀번호는 8자 이상 20자 이하여야 합니다.";
    public static final String PASSWORD_CANNOT_CONTAINS_BLANK = "비밀번호는 공백을 포함할 수 없습니다.";
    public static final String PASSWORD_MUST_CONTAIN_UPPERCASE = "비밀번호에는 대문자가 최소 1개 포함되어야 합니다.";
    public static final String PASSWORD_MUST_CONTAIN_LOWERCASE = "비밀번호에는 소문자가 최소 1개 포함되어야 합니다.";
    public static final String PASSWORD_MUST_CONTAIN_NUMBER = "비밀번호에는 숫자를 최소 1개 포함되어야 합니다.";
    public static final String PASSWORD_MUST_CONTAIN_SPECIAL_LETTER = "비밀번호에는 특수문자가 최소 1개 포함되어야 합니다.";
    public static final String PASSWORD_INCORRECT = "비밀번호가 일치하지 않습니다.";
    public static final String PASSWORD_ALREADY_USED = "이미 사용하고 있는 비밀번호입니다.";
}
