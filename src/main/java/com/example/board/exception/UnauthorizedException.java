package com.example.board.exception;

public class UnauthorizedException extends BusinessException{
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
