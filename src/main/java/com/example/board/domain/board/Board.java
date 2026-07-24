package com.example.board.domain.board;

import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Board {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;
    private boolean isDeleted;
    private Integer numberOfLikes;
    private Integer numberOfViews;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    @OrderBy("registDate ASC, id ASC")
    private List<BoardModifyRecord> boardModifyRecords = new ArrayList<>();

    @OneToMany(mappedBy = "viewedBoard")
    private List<BoardViewRecord> boardViewRecords = new ArrayList<>();

    @OneToMany(mappedBy = "likedBoard")
    private List<BoardLikeRecord> boardLikeRecords = new ArrayList<>();

    @OneToMany(mappedBy = "reportedBoard")
    private List<BoardReportRecord> boardReportRecords = new ArrayList<>();

    protected Board() {}

    private Board(User author){
        this.isDeleted = false;
        this.numberOfLikes = 0;
        this.numberOfViews = 0;
        this.author = author;
    }

    public static Board create(User author, String title, String content, List<String> imageUrls) {
        Board board = new Board(author);
        author.addBoard(board);
        board.addModifyRecord(title, content, imageUrls);
        return board;
    }

    public void deleteBoard(){
        this.isDeleted = true;
    }

    public void increaseNumberOfLikes(){
        this.numberOfLikes++;
    }

    public void decreaseNumberOfLikes(){
        if (this.numberOfLikes > 0) {
            this.numberOfLikes--;
        }
    }

    public void increaseNumberOfViews(){
        this.numberOfViews++;
    }

    public void addModifyRecord(String title, String content, List<String> imageUrls) {
        this.boardModifyRecords.add(BoardModifyRecord.create(this, title, content, imageUrls));
    }

    public void addBoardViewRecord(BoardViewRecord boardViewRecord) {
        this.boardViewRecords.add(boardViewRecord);
    }

    public void addBoardLikeRecord(BoardLikeRecord boardLikeRecord){
        this.boardLikeRecords.add(boardLikeRecord);
    }

    public void removeBoardLikeRecord(BoardLikeRecord boardLikeRecord){
        this.boardLikeRecords.remove(boardLikeRecord);
    }

    public void addBoardReportRecord(BoardReportRecord boardReportRecord){
        this.boardReportRecords.add(boardReportRecord);
    }

    public void removeBoardReportRecord(BoardReportRecord boardReportRecord){
        this.boardReportRecords.remove(boardReportRecord);
    }
}
