package com.example.board.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ApiResponse<T> { //data의 타입을 외부에서 정하도록 함

    private final String code;
    private final T data;

    public static <T> ApiResponse<T> of(String code, T data){   //클래스 없이 생성하는 static메소드 타입 추론 위해 <T>을 사용함을 메소드에 명시
        return new ApiResponse<>(code, data);   //컴파일러가 data의 타입을 보고 유추하기에 <> 사용
    }
}
