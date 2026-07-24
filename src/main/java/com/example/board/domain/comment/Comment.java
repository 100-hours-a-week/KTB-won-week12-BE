package com.example.board.domain.comment;

import com.example.board.domain.board.Board;
import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;
    private Boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL)
    @OrderBy("registDate ASC, id ASC")
    private List<CommentModifyRecord> commentModifyRecords = new ArrayList<>();

    @OneToMany(mappedBy = "likedComment")
    private List<CommentLikeRecord> commentLikeRecords = new ArrayList<>();

    @OneToMany(mappedBy = "reportedComment")
    private List<CommentReportRecord> commentReportedRecords = new ArrayList<>();

    protected Comment() {}

    private Comment(User author, Board board){
        this.author = author;
        this.board = board;
        this.isDeleted = false;
    }

    public static Comment create(User author, Board board, String content) {
        Comment comment = new Comment(author, board);
        comment.addModifyRecord(content);
        return comment;
    }

    public void addModifyRecord(String content){
        this.commentModifyRecords.add(CommentModifyRecord.create(this, content));
    }

    public void deleteComment() {
        this.isDeleted = true;
    }

    public void addCommentLikedRecord(CommentLikeRecord commentLikeRecord){
        this.commentLikeRecords.add(commentLikeRecord);
    }

    public void removeCommentLikedRecord(CommentLikeRecord commentLikeRecord){
        this.commentLikeRecords.remove(commentLikeRecord);
    }

    public void addCommentReportedRecord(CommentReportRecord commentReportRecord){
        this.commentReportedRecords.add(commentReportRecord);
    }

    public void removeCommentReportedRecord(CommentReportRecord commentReportRecord){
        this.commentReportedRecords.remove(commentReportRecord);
    }
}
