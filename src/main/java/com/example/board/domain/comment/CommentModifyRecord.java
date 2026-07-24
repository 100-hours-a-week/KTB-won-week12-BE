package com.example.board.domain.comment;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class CommentModifyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_modify_record_id")
    private Long id;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private LocalDateTime registDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    Comment comment;

    protected CommentModifyRecord() {}

    private CommentModifyRecord(String content, Comment comment){
        this.content = content;
        this.registDate = LocalDateTime.now();
        this.comment = comment;
    }

    public static CommentModifyRecord create(Comment comment, String content) {
        return new CommentModifyRecord(content, comment);
    }
}
