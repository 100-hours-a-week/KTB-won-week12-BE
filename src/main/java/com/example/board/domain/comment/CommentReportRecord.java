package com.example.board.domain.comment;

import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_report_record_user_comment",
                        columnNames = {"user_id", "comment_id"}
                )
        }
)
public class CommentReportRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_report_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment reportedComment;

    protected CommentReportRecord() {}

    public CommentReportRecord(User reportedUser, Comment reportedComment){
        this.reportedUser = reportedUser;
        this.reportedComment = reportedComment;
    }
}
