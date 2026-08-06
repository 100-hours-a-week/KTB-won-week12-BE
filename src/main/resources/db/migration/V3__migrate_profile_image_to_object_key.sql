-- 프로필 이미지는 만료되는 URL 대신 S3에서 객체를 식별하는 Object Key를 저장한다.
-- 기존 profile_image 값은 없으므로 데이터 변환 없이 컬럼명과 최대 길이만 변경한다.
ALTER TABLE users
    CHANGE COLUMN profile_image profile_image_object_key VARCHAR(512) NULL;
