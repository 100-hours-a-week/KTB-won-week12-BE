package com.example.board.domain.board;

import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_report_record_user_board",
                        columnNames = {"user_id", "board_id"}
                )
        }

)
public class BoardReportRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_report_record_id")
    private Long id;

    private String reportReason;
    private Boolean isReportHandled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board reportedBoard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User reportedUser;

    protected BoardReportRecord() {}

    public BoardReportRecord(String reportReason, Board reportedBoard, User reportedUser){
        this.reportReason = reportReason;
        this.isReportHandled = false;
        this.reportedBoard = reportedBoard;
        this.reportedUser = reportedUser;
    }
}
