package com.example.board.domain.board;

import com.example.board.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_like_record_user_board",
                        columnNames = {"user_id", "board_id"}
                )
        }
)
public class BoardLikeRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_like_record_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User likedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board likedBoard;

    protected BoardLikeRecord() {}

    public BoardLikeRecord(User likedUser, Board likedBoard){
        this.likedUser = likedUser;
        this.likedBoard = likedBoard;
    }
}
