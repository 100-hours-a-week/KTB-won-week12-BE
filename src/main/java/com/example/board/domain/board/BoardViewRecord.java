package com.example.board.domain.board;

import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_view_record_user_board",
                        columnNames = {"user_id", "board_id"}
                )
        }
)
public class BoardViewRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_view_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User viewedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    Board viewedBoard;

    protected BoardViewRecord() {}

    public BoardViewRecord(User viewedUser, Board viewedBoard){
        this.viewedUser = viewedUser;
        this.viewedBoard = viewedBoard;
    }
}