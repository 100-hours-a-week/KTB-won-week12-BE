package com.example.board.repository;

import java.time.LocalDateTime;

public interface CommentCreatedAtProjection {
    Long getCommentId();
    LocalDateTime getCreatedAt();
}
