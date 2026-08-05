ALTER TABLE board_image
    CHANGE COLUMN image_url original_object_key VARCHAR(512) NOT NULL,
    ADD COLUMN thumbnail_object_key VARCHAR(512) NOT NULL AFTER original_object_key;
