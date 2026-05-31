package org.ktb.sideproject.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String JwtSecret;
    @Value("${jwt.access-token-exp-seconds}")
    private long accessTokenExpSeconds;
    @Value("${jwt.refresh-token-exp-seconds}")
    private long refreshTokenExpSeconds;

    private static final String AT = "accessToken";
    private static final String RT = "refreshToken";

    //AT 생성
    public String createAT(Long userId) {
        return this.createToken(userId, accessTokenExpSeconds, AT);
    }
    //RT 생성
    public String createRT(Long userId) {
        return this.createToken(userId, refreshTokenExpSeconds, RT);
    }
    // 토큰에서 사용자 추출
    public Long getUserId(String token) {
        return Long.valueOf(this.parseToken(token).getSubject());
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, AT);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, RT);
    }

    // 토큰 검증
    private boolean validateToken(String token, String tokenType){
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);
            return tokenType.equals(type);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    // 토큰 생성
    private String createToken(Long userId, long expireSeconds, String type) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000L);

        SecretKey key = Keys.hmacShaKeyFor(JwtSecret.getBytes(StandardCharsets.UTF_8));

        /*
         *   토큰의 들어가는 것
         *   사용자 정보(userId_
         *   토큰 타입(AT, RT)
         *   지금 시간 now
         *   만료 시간 expiration
         */
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    // 토큰 파서
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(JwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
