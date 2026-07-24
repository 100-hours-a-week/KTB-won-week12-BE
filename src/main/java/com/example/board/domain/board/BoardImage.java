package com.example.board.domain.board;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class BoardImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_image_id")
    private Long id;
    @Column(nullable = false, length = 2048)
    private String imageUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_modify_record_id")
    private BoardModifyRecord boardModifyRecord;

    protected BoardImage() {}

    public BoardImage(String imageUrl, BoardModifyRecord boardModifyRecord){
        this.imageUrl = imageUrl;
        this.boardModifyRecord = boardModifyRecord;
    }
}
