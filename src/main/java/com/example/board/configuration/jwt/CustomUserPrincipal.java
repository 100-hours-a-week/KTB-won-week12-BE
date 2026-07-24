package com.example.board.configuration.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CustomUserPrincipal{
    private final Long userId;
    private final String email;
    private final String authorities;
}
