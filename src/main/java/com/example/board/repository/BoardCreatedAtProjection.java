package com.example.board.repository;

import java.time.LocalDateTime;

public interface BoardCreatedAtProjection {
    Long getBoardId();
    LocalDateTime getCreatedAt();
}
