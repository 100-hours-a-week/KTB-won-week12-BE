package com.example.board.exception.errorMessage;

public class UserErrorMessage {
    //닉네임 에러메세지
    public static final String NICKNAME_REQUIRED = "닉네임은 필수 입력값입니다.";
    public static final String NICKNAME_LENGTH_LIMIT = "닉네임은 2글자 이상 10글자 이하여야합니다.";
    public static final String NICKNAME_ALREADY_EXISTS = "이미 존재하는 닉네임입니다.";

    //이메일 에러메세지
    public static final String EMAIL_REQUIRED = "이메일은 필수 입력값입니다.";
    public static final String EMAIL_FORM_INCORRECT = "이메일 형식이 올바르지 않습니다.";
    public static final String EMAIL_ALREADY_EXISTS = "이미 존재하는 이메일입니다.";

    //사용자 조회 에러메세지
    public static final String USER_NOT_FOUND = "사용자가 존재하지 않습니다.";
}
