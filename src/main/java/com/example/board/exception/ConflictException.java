package com.example.board.exception;

public class ConflictException extends BusinessException{

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
