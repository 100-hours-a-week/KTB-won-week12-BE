CREATE TABLE users (
    is_deleted BIT,
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    delete_reason VARCHAR(255),
    email VARCHAR(255),
    nickname VARCHAR(255),
    password VARCHAR(255),
    profile_image VARCHAR(255),
    user_role ENUM ('ADMIN', 'USER'),
    PRIMARY KEY (user_id)
) ENGINE=InnoDB;

CREATE TABLE board (
    is_deleted BIT NOT NULL,
    number_of_likes INTEGER,
    number_of_views INTEGER,
    board_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    PRIMARY KEY (board_id),
    CONSTRAINT FK5vlh90qyii65ixwsbnafd55ud
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE board_modify_record (
    board_id BIGINT,
    board_modify_record_id BIGINT NOT NULL AUTO_INCREMENT,
    regist_date DATETIME(6) NOT NULL,
    title VARCHAR(26) NOT NULL,
    content TINYTEXT NOT NULL,
    PRIMARY KEY (board_modify_record_id),
    CONSTRAINT FKpv7llfvbve6gg7q6k74lyxqkd
        FOREIGN KEY (board_id) REFERENCES board (board_id)
) ENGINE=InnoDB;

CREATE TABLE board_image (
    board_image_id BIGINT NOT NULL AUTO_INCREMENT,
    board_modify_record_id BIGINT,
    image_url VARCHAR(2048) NOT NULL,
    PRIMARY KEY (board_image_id),
    CONSTRAINT FK5j49vgga1ghfo8264lncvxklf
        FOREIGN KEY (board_modify_record_id)
        REFERENCES board_modify_record (board_modify_record_id)
) ENGINE=InnoDB;

CREATE TABLE board_like_record (
    board_id BIGINT,
    board_like_record_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    PRIMARY KEY (board_like_record_id),
    CONSTRAINT uk_board_like_record_user_board UNIQUE (user_id, board_id),
    CONSTRAINT FKkldxnrb793em9sep4dtgduto6
        FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT FKsuu5c7d0c7kunpxtt8owoy35y
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE board_report_record (
    is_report_handled BIT,
    board_id BIGINT,
    board_report_record_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    report_reason VARCHAR(255),
    PRIMARY KEY (board_report_record_id),
    CONSTRAINT uk_board_report_record_user_board UNIQUE (user_id, board_id),
    CONSTRAINT FK4314im0cfin1hlge6ed5h1m06
        FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT FKao68mr3tqmd038j9au6r6ap7k
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE board_view_record (
    board_id BIGINT,
    board_view_record_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    PRIMARY KEY (board_view_record_id),
    CONSTRAINT uk_board_view_record_user_board UNIQUE (user_id, board_id),
    CONSTRAINT FKd7oshymi9i1pxudfp8sqbyri0
        FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT FKehn2h6sisvvgel4mhsuukyybv
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE comment (
    is_deleted BIT,
    board_id BIGINT,
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    parent_comment_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (comment_id),
    CONSTRAINT FKqm52p1v3o13hy268he0wcngr5
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT FKlij9oor1nav89jeat35s6kbp1
        FOREIGN KEY (board_id) REFERENCES board (board_id),
    CONSTRAINT FKhvh0e2ybgg16bpu229a5teje7
        FOREIGN KEY (parent_comment_id) REFERENCES comment (comment_id)
) ENGINE=InnoDB;

CREATE TABLE comment_like_record (
    comment_id BIGINT,
    comment_like_record_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    PRIMARY KEY (comment_like_record_id),
    CONSTRAINT uk_comment_like_record_user_comment UNIQUE (user_id, comment_id),
    CONSTRAINT FKtf2t3413wj7ljd1y84rop7cuu
        FOREIGN KEY (comment_id) REFERENCES comment (comment_id),
    CONSTRAINT FKtl25jc6m7qvp5v26l2ft0fcra
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;

CREATE TABLE comment_modify_record (
    comment_id BIGINT,
    comment_modify_record_id BIGINT NOT NULL AUTO_INCREMENT,
    regist_date DATETIME(6) NOT NULL,
    content TINYTEXT NOT NULL,
    PRIMARY KEY (comment_modify_record_id),
    CONSTRAINT FK99380d0x6kq8e20m764omqfwr
        FOREIGN KEY (comment_id) REFERENCES comment (comment_id)
) ENGINE=InnoDB;

CREATE TABLE comment_report_record (
    comment_id BIGINT,
    comment_report_record_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    PRIMARY KEY (comment_report_record_id),
    CONSTRAINT uk_comment_report_record_user_comment UNIQUE (user_id, comment_id),
    CONSTRAINT FKqoe2l8s9tl3e0sd9md9jptm1j
        FOREIGN KEY (comment_id) REFERENCES comment (comment_id),
    CONSTRAINT FK4r90qhrh04ucf3690q2xv3wu
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB;
