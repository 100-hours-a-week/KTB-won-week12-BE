package com.example.board.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String EMAIL_CLAIM = "email";
    private static final String AUTHORITY_CLAIM = "authority";

    // JWT 서명 생성 및 검증에 사용하는 비밀키
    private final SecretKey signingKey;

    // JWT를 파싱하고 검증하는 객체
    private final JwtParser jwtParser;

    // Access Token 만료 시간
    private final Duration accessTokenExpiration;

    // Refresh Token 만료 시간
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(JwtProperties jwtProperties) {


        byte[] keyBytes =
                Decoders.BASE64.decode(jwtProperties.secret());

        //byte 배열을 JWT 서명용 SecretKey로 변환
        this.signingKey =
                Keys.hmacShaKeyFor(keyBytes);

        this.accessTokenExpiration =
                jwtProperties.accessTokenExpiration();

        this.refreshTokenExpiration =
                jwtProperties.refreshTokenExpiration();

        // 토큰을 검증할 JwtParser 생성
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .build();
    }

    public String createAccessToken(Long userId, String email, String authority) {
        // 토큰 발급 시간
        Instant issuedAt = Instant.now();

        // 토큰 만료 시간
        Instant expiresAt =
                issuedAt.plus(accessTokenExpiration);

        return Jwts.builder()

                //userId를 sub로 설정
                .subject(userId.toString())

                //email
                .claim(EMAIL_CLAIM, email)

                // 이 토큰이 Access Token임을 표시
                .claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS.getValue())

                // Spring Security에서 사용할 사용자 권한
                .claim(AUTHORITY_CLAIM, authority)

                // 토큰 발급 시간
                .issuedAt(Date.from(issuedAt))

                // 토큰 만료 시간
                .expiration(Date.from(expiresAt))

                // signingKey를 사용하여 JWT에 서명
                .signWith(signingKey)

                // 최종 JWT 문자열 생성
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant issuedAt = Instant.now();

        Instant expiresAt =
                issuedAt.plus(refreshTokenExpiration);

        return Jwts.builder()

                // 사용자 ID
                .subject(userId.toString())

                // 이 토큰이 Refresh Token임을 표시
                .claim(
                        TOKEN_TYPE_CLAIM,
                        TokenType.REFRESH.getValue()
                )

                // 발급 시간
                .issuedAt(Date.from(issuedAt))

                // 만료 시간
                .expiration(Date.from(expiresAt))

                // 서명
                .signWith(signingKey)

                // JWT 문자열 생성
                .compact();
    }


    public Claims validateAccessToken(String token) {
        return parseAndValidateToken(
                token,
                TokenType.ACCESS
        );
    }


    public Claims validateRefreshToken(String token) {
        return parseAndValidateToken(
                token,
                TokenType.REFRESH
        );
    }

    public CustomUserPrincipal getCustomUserPrincipal(String token){
        Claims claims = parseClaims(token);

        Long userId = Long.valueOf(claims.getSubject());
        String email = String.valueOf(claims.get("email", String.class));
        String authority = String.valueOf(claims.get("authority", String.class));
        return new CustomUserPrincipal(userId, email, authority);
    }

    public Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getAuthority(Claims claims) {
        return claims.get(
                AUTHORITY_CLAIM,
                String.class
        );
    }

    private Claims parseAndValidateToken(
            String token,
            TokenType expectedTokenType
    ) {
        // 토큰이 없거나 빈 문자열이면 검증하지 않는다.
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT 토큰이 비어 있습니다."
            );
        }

        Claims claims = jwtParser
                .parseSignedClaims(token)
                .getPayload();


        String actualTokenType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        if (!expectedTokenType
                .getValue()
                .equals(actualTokenType)) {

            throw new JwtException(
                    "올바르지 않은 JWT 토큰 종류입니다."
            );
        }

        return claims;
    }
}
