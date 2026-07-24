package com.example.board.dto.errorDTO;

import com.example.board.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final Object data;

    private ErrorResponse(ErrorCode errorCode, Object data){
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
        this.data = data;
    }

    public static ErrorResponse of(ErrorCode errorCode){
        return new ErrorResponse(errorCode, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Object data){
        return new ErrorResponse(errorCode, data);
    }
}
