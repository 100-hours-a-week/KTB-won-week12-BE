package com.example.board.repository;

public interface BoardCommentCountProjection {
    Long getBoardId();
    long getCommentCount();
}
