package com.example.board.domain.comment;

import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_like_record_user_comment",
                        columnNames = {"user_id", "comment_id"}
                )
        }
)
public class CommentLikeRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "comment_like_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User likedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment likedComment;

    protected CommentLikeRecord() {}

    public CommentLikeRecord(User likedUser, Comment likedComment){
        this.likedUser = likedUser;
        this.likedComment = likedComment;
    }
}
