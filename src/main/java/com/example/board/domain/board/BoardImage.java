package com.example.board.domain.board;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class BoardImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_image_id")
    private Long id;
    @Column(nullable = false, length = 512)
    private String originalObjectKey;
    @Column(nullable = false, length = 512)
    private String thumbnailObjectKey;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_modify_record_id")
    private BoardModifyRecord boardModifyRecord;

    protected BoardImage() {}

    public BoardImage(BoardImageKeys imageKeys, BoardModifyRecord boardModifyRecord){
        this.originalObjectKey = imageKeys.originalObjectKey();
        this.thumbnailObjectKey = imageKeys.thumbnailObjectKey();
        this.boardModifyRecord = boardModifyRecord;
    }
}
