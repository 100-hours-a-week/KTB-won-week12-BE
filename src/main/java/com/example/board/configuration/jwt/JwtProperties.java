package com.example.board.configuration.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        // JWT 서명에 사용할 Base64 비밀키
        String secret,

        // Access Token 만료 시간
        Duration accessTokenExpiration,

        // Refresh Token 만료 시간
        Duration refreshTokenExpiration

) {
}