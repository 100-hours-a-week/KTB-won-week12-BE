package com.example.board.domain.board;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class BoardModifyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_modify_record_id")
    private Long id;
    @Column(nullable = false, length = 26)
    private String title;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private LocalDateTime registDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @OneToMany(mappedBy = "boardModifyRecord", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<BoardImage> boardImage = new ArrayList<>();

    protected BoardModifyRecord() {}

    private BoardModifyRecord (Board board, String title, String content){
        this.board = board;
        this.title = title;
        this.content = content;
        this.registDate = LocalDateTime.now();
    }

    public static BoardModifyRecord create(Board board, String title, String content, List<String> imageUrls) {
        BoardModifyRecord modifyRecord = new BoardModifyRecord(board, title, content);
        imageUrls.forEach(imageUrl -> modifyRecord.addBoardImage(new BoardImage(imageUrl, modifyRecord)));
        return modifyRecord;
    }

    public void addBoardImage(BoardImage boardImage){
        this.boardImage.add(boardImage);
    }
}
