package com.example.board.exception.errorMessage;

public final class BoardErrorMessage {

    public static final String TITLE_REQUIRED = "게시글 제목은 필수 입력값입니다.";
    public static final String TITLE_LENGTH_LIMIT = "게시글 제목은 26자 이하여야 합니다.";
    public static final String CONTENT_REQUIRED = "게시글 내용은 필수 입력값입니다.";
    public static final String IMAGE_URL_REQUIRED = "이미지 URL은 빈 값일 수 없습니다.";
    public static final String IMAGE_URL_LENGTH_LIMIT = "이미지 URL은 2048자 이하여야 합니다.";
    public static final String IMAGE_URL_INVALID = "이미지 URL은 http 또는 https 형식이어야 합니다.";
    public static final String BOARD_NOT_FOUND = "게시글이 존재하지 않습니다.";
    public static final String CURSOR_INVALID = "게시글 cursor는 양수여야 합니다.";
    public static final String PAGE_SIZE_INVALID = "게시글 조회 크기는 1 이상 50 이하여야 합니다.";
    public static final String BOARD_MODIFY_FORBIDDEN = "게시글 작성자만 수정하거나 삭제할 수 있습니다.";

    private BoardErrorMessage() {
    }
}
