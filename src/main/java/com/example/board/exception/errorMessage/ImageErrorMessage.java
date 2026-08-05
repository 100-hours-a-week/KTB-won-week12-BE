package com.example.board.exception.errorMessage;

public final class ImageErrorMessage {

    public static final String IMAGE_COUNT_LIMIT = "게시글 이미지는 최대 5개까지 첨부할 수 있습니다.";
    public static final String FILE_NAME_REQUIRED = "이미지 파일 이름은 필수입니다.";
    public static final String FILE_NAME_LENGTH_LIMIT = "이미지 파일 이름은 255자 이하여야 합니다.";
    public static final String CONTENT_TYPE_INVALID = "JPEG, PNG, WebP 이미지만 첨부할 수 있습니다.";
    public static final String ORIGINAL_SIZE_INVALID = "원본 이미지는 5MB 이하여야 합니다.";
    public static final String THUMBNAIL_TYPE_INVALID = "썸네일은 WebP 형식이어야 합니다.";
    public static final String THUMBNAIL_SIZE_INVALID = "썸네일은 1MB 이하여야 합니다.";
    public static final String OBJECT_KEY_REQUIRED = "이미지 Object Key는 필수입니다.";
    public static final String OBJECT_KEY_LENGTH_LIMIT = "이미지 Object Key는 512자 이하여야 합니다.";
    public static final String OBJECT_KEY_INVALID = "사용할 수 없는 이미지 Object Key입니다.";
    public static final String OBJECT_NOT_FOUND = "업로드된 이미지를 찾을 수 없습니다.";
    public static final String STORAGE_UNAVAILABLE = "이미지 저장소에 일시적인 문제가 발생했습니다.";

    private ImageErrorMessage() {
    }
}
