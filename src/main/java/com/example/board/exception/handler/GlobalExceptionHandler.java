package com.example.board.exception.handler;

import com.example.board.dto.errorDTO.ErrorResponse;
import com.example.board.exception.BusinessException;
import com.example.board.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception){

        return ResponseEntity
                .status(exception.getHttpStatus())
                .body(ErrorResponse.of(exception.getErrorCode()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity
                .status(ErrorCode.LOGIN_FAILED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.LOGIN_FAILED));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwt(JwtException exception) {
        return ResponseEntity
                .status(ErrorCode.AUTHENTICATION_FAILED.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.AUTHENTICATION_FAILED));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST_BODY.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST_BODY));
    }

    @ExceptionHandler(Exception.class)  //예기치 못한 500에러 핸들링
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request){

        log.error(
                "처리되지 않은 서버 예외. method = {}, uri = {}", request.getMethod(), request.getRequestURI(),exception
        );

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception){

        ErrorCode errorCode = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .map(ErrorCode::fromMessage)
                .orElse(ErrorCode.INVALID_INPUT);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }
}
