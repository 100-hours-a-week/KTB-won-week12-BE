package com.example.board.exception.errorMessage;

public final class CommentErrorMessage {

    public static final String CONTENT_REQUIRED = "댓글 내용은 필수 입력값입니다.";
    public static final String COMMENT_NOT_FOUND = "댓글이 존재하지 않습니다.";
    public static final String COMMENT_MODIFY_FORBIDDEN = "댓글 작성자만 수정하거나 삭제할 수 있습니다.";
    public static final String CURSOR_INVALID = "댓글 cursor는 양수여야 합니다.";
    public static final String PAGE_SIZE_INVALID = "댓글 조회 크기는 1 이상 50 이하여야 합니다.";

    private CommentErrorMessage() {
    }
}
