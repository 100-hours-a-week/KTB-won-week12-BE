-- Java int 필드의 Hibernate INTEGER 매핑과 운영 MySQL 컬럼 타입을 일치시킨다.
-- 이미 적용될 수 있는 V4의 checksum을 변경하지 않고 후속 마이그레이션으로 보정한다.
ALTER TABLE board_vote_response
    MODIFY COLUMN left_score INTEGER NOT NULL;
