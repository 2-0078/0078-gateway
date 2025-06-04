package com.pieceofcake.gateway.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtProvider {

    private SecretKey secretKey;

    public JwtProvider(Environment env) {
        String secret = env.getProperty("JWT.secret-key");

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("❗ JWT.secret-key is not set.");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("✅ JWT SecretKey loaded successfully.");
    }

    // 유효한 토큰인지 확인
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // 예외 발생 시 catch됨
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT invalid: {}", e.getMessage());
            return false;
        }
    }

    // 만료 여부
    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    // 토큰에서 Claims 추출
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰에서 memberUuid 추출
    public String getMemberUuid(String token) {
        Claims claims = parseClaims(token);
        return claims.get("memberUuid", String.class);
    }
}