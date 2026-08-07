package com.example.board.exception;

import com.example.board.exception.errorMessage.AuthErrorMessage;
import com.example.board.exception.errorMessage.BoardErrorMessage;
import com.example.board.exception.errorMessage.CommentErrorMessage;
import com.example.board.exception.errorMessage.CommonErrorMessage;
import com.example.board.exception.errorMessage.UserErrorMessage;
import com.example.board.exception.errorMessage.ImageErrorMessage;
import com.example.board.exception.errorMessage.VoteErrorMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, CommonErrorMessage.INVALID_INPUT),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, CommonErrorMessage.INVALID_REQUEST_BODY),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorMessage.INTERNAL_SERVER_ERROR),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, AuthErrorMessage.LOGIN_FAILED),
    USER_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, AuthErrorMessage.USER_UNAUTHENTICATED),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, AuthErrorMessage.AUTHENTICATION_FAILED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, AuthErrorMessage.USER_UNAUTHORIZED),
    USER_DELETED(HttpStatus.UNAUTHORIZED, AuthErrorMessage.USER_DELETED),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_REQUIRED),
    PASSWORD_LENGTH_LIMIT(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_LENGTH_LIMIT),
    PASSWORD_CANNOT_CONTAIN_BLANK(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_CANNOT_CONTAINS_BLANK),
    PASSWORD_MUST_CONTAIN_UPPERCASE(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_MUST_CONTAIN_UPPERCASE),
    PASSWORD_MUST_CONTAIN_LOWERCASE(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_MUST_CONTAIN_LOWERCASE),
    PASSWORD_MUST_CONTAIN_NUMBER(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_MUST_CONTAIN_NUMBER),
    PASSWORD_MUST_CONTAIN_SPECIAL_LETTER(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_MUST_CONTAIN_SPECIAL_LETTER),
    PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_INCORRECT),
    PASSWORD_ALREADY_USED(HttpStatus.BAD_REQUEST, AuthErrorMessage.PASSWORD_ALREADY_USED),

    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, UserErrorMessage.NICKNAME_REQUIRED),
    NICKNAME_LENGTH_LIMIT(HttpStatus.BAD_REQUEST, UserErrorMessage.NICKNAME_LENGTH_LIMIT),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, UserErrorMessage.NICKNAME_ALREADY_EXISTS),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, UserErrorMessage.EMAIL_REQUIRED),
    EMAIL_FORM_INCORRECT(HttpStatus.BAD_REQUEST, UserErrorMessage.EMAIL_FORM_INCORRECT),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, UserErrorMessage.EMAIL_ALREADY_EXISTS),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, UserErrorMessage.USER_NOT_FOUND),

    BOARD_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, BoardErrorMessage.TITLE_REQUIRED),
    BOARD_TITLE_LENGTH_LIMIT(HttpStatus.BAD_REQUEST, BoardErrorMessage.TITLE_LENGTH_LIMIT),
    BOARD_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, BoardErrorMessage.CONTENT_REQUIRED),
    BOARD_IMAGE_COUNT_LIMIT(HttpStatus.BAD_REQUEST, ImageErrorMessage.IMAGE_COUNT_LIMIT),
    IMAGE_FILE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, ImageErrorMessage.FILE_NAME_REQUIRED),
    IMAGE_FILE_NAME_LENGTH_LIMIT(HttpStatus.BAD_REQUEST, ImageErrorMessage.FILE_NAME_LENGTH_LIMIT),
    IMAGE_CONTENT_TYPE_INVALID(HttpStatus.BAD_REQUEST, ImageErrorMessage.CONTENT_TYPE_INVALID),
    IMAGE_ORIGINAL_SIZE_INVALID(HttpStatus.BAD_REQUEST, ImageErrorMessage.ORIGINAL_SIZE_INVALID),
    IMAGE_THUMBNAIL_TYPE_INVALID(HttpStatus.BAD_REQUEST, ImageErrorMessage.THUMBNAIL_TYPE_INVALID),
    IMAGE_THUMBNAIL_SIZE_INVALID(HttpStatus.BAD_REQUEST, ImageErrorMessage.THUMBNAIL_SIZE_INVALID),
    IMAGE_OBJECT_KEY_REQUIRED(HttpStatus.BAD_REQUEST, ImageErrorMessage.OBJECT_KEY_REQUIRED),
    IMAGE_OBJECT_KEY_LENGTH_LIMIT(HttpStatus.BAD_REQUEST, ImageErrorMessage.OBJECT_KEY_LENGTH_LIMIT),
    IMAGE_OBJECT_KEY_INVALID(HttpStatus.BAD_REQUEST, ImageErrorMessage.OBJECT_KEY_INVALID),
    IMAGE_OBJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, ImageErrorMessage.OBJECT_NOT_FOUND),
    IMAGE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, ImageErrorMessage.STORAGE_UNAVAILABLE),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, BoardErrorMessage.BOARD_NOT_FOUND),
    BOARD_CURSOR_INVALID(HttpStatus.BAD_REQUEST, BoardErrorMessage.CURSOR_INVALID),
    BOARD_PAGE_SIZE_INVALID(HttpStatus.BAD_REQUEST, BoardErrorMessage.PAGE_SIZE_INVALID),
    BOARD_MODIFY_FORBIDDEN(HttpStatus.FORBIDDEN, BoardErrorMessage.BOARD_MODIFY_FORBIDDEN),

    VOTE_SCORE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, VoteErrorMessage.SCORE_OUT_OF_RANGE),
    VOTE_LABEL_REQUIRED(HttpStatus.BAD_REQUEST, VoteErrorMessage.LABEL_REQUIRED),
    VOTE_LABEL_LENGTH_LIMIT(HttpStatus.BAD_REQUEST, VoteErrorMessage.LABEL_LENGTH_LIMIT),
    VOTE_LABEL_DUPLICATED(HttpStatus.BAD_REQUEST, VoteErrorMessage.LABEL_DUPLICATED),
    VOTE_DURATION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, VoteErrorMessage.DURATION_OUT_OF_RANGE),
    BOARD_VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, VoteErrorMessage.VOTE_NOT_FOUND),
    BOARD_VOTE_CLOSED(HttpStatus.CONFLICT, VoteErrorMessage.VOTE_CLOSED),

    COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, CommentErrorMessage.CONTENT_REQUIRED),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, CommentErrorMessage.COMMENT_NOT_FOUND),
    COMMENT_MODIFY_FORBIDDEN(HttpStatus.FORBIDDEN, CommentErrorMessage.COMMENT_MODIFY_FORBIDDEN),
    COMMENT_CURSOR_INVALID(HttpStatus.BAD_REQUEST, CommentErrorMessage.CURSOR_INVALID),
    COMMENT_PAGE_SIZE_INVALID(HttpStatus.BAD_REQUEST, CommentErrorMessage.PAGE_SIZE_INVALID);

    private final HttpStatus httpStatus;
    private final String message;

    public static ErrorCode fromMessage(String message) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.message.equals(message))
                .findFirst()
                .orElse(INVALID_INPUT);
    }
}
