-- 게시글에는 투표 연관관계를 두지 않고, 투표 테이블이 게시글을 참조한다.
-- UNIQUE 제약조건으로 JPA에서는 N:1이어도 실제 데이터는 게시글당 최대 한 건만 허용한다.
CREATE TABLE board_vote (
    board_vote_id BIGINT NOT NULL AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    left_label VARCHAR(20) NOT NULL,
    right_label VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    PRIMARY KEY (board_vote_id),
    CONSTRAINT uk_board_vote_board UNIQUE (board_id),
    CONSTRAINT fk_board_vote_board
        FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT ck_board_vote_labels_different CHECK (left_label <> right_label),
    CONSTRAINT ck_board_vote_period CHECK (ends_at > started_at)
) ENGINE=InnoDB;

CREATE TABLE board_vote_response (
    board_vote_response_id BIGINT NOT NULL AUTO_INCREMENT,
    board_vote_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    left_score TINYINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (board_vote_response_id),
    -- 동일 사용자의 재투표는 새 행 추가가 아니라 기존 응답 갱신으로 처리한다.
    CONSTRAINT uk_board_vote_response_vote_user UNIQUE (board_vote_id, user_id),
    CONSTRAINT fk_board_vote_response_vote
        FOREIGN KEY (board_vote_id) REFERENCES board_vote (board_vote_id),
    CONSTRAINT fk_board_vote_response_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_board_vote_response_left_score CHECK (left_score BETWEEN 0 AND 10)
) ENGINE=InnoDB;
