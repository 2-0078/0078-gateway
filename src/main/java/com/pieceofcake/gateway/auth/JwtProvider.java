package com.pieceofcake.gateway.auth;

import com.pieceofcake.gateway.common.exception.BaseException;
import com.pieceofcake.gateway.common.exception.BaseResponseStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtProvider {

    private final Environment env;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // jwt secret key를 byte 배열로 변환하여 Key 객체 생성
        String secret = Objects.requireNonNull(env.getProperty("JWT.secret-key"));
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 토큰 유효성 검증 (서명 확인 및 만료 시간 체크)
     * @param token
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims.getExpiration().before(new Date())) {
                log.warn("❌ Token expired at {}", claims.getExpiration());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("❌ Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public String getMemberUuid(String token) {
        Claims claims = extractAllClaims(token);
        String memberUuid = claims.get("memberUuid", String.class);
        if (memberUuid == null) {
            throw new BaseException(BaseResponseStatus.NO_ACCESS_AUTHORITY);
        }
        return memberUuid;
    }

    public String getMemberRole(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get("role", String.class); // roles가 String이라면
        if (role == null) {
            throw new BaseException(BaseResponseStatus.NO_ACCESS_AUTHORITY);
        }
        return role;
    }
//    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
//        Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}