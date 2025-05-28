package com.pieceofcake.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthorizationFilter implements GatewayFilter {

    // application.yml에서 JWT 서명 키 값을 주입받음
    @Value("${auth.jwt.key}")
    private String keyString;

    // JWT 검증에 사용할 SecretKey 객체
    private SecretKey secretKey;

    // JSON 변환을 위한 ObjectMapper
    private final ObjectMapper objectMapper;

    // Bearer 타입 prefix와 사용자 식별자 헤더 정의
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_MEMBER_UUID = "X-Member-Uuid";

    // 필터 초기화 시 JWT SecretKey 객체로 변환
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
    }

    // 필터의 핵심 메서드
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("JWT Authorization Filter");

        try {
            // 1. Authorization 헤더 가져오기
            List<String> authorizationHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader == null || authorizationHeader.isEmpty()) {
                // 헤더가 없으면 701 코드 반환
                return sendError(exchange, 701, "Authorization header is missing");
            }

            // 2. Bearer 타입의 토큰만 추출
            String bearerToken = authorizationHeader.stream()
                    .filter(header -> header.startsWith(BEARER_PREFIX))
                    .findFirst()
                    .orElse(null);

            if (bearerToken == null) {
                // Bearer 토큰이 없으면 702 코드 반환
                return sendError(exchange, 702, "Bearer token is missing");
            }

            // 3. Bearer 접두사 제거하여 실제 토큰 추출
            String jwt = bearerToken.substring(BEARER_PREFIX.length());

            // 4. JWT 파싱 및 Claims 추출
            Claims claims = getClaims(jwt);

            // 5. 토큰 만료 여부 확인
            if (isExpired(claims.getExpiration())) {
                return sendError(exchange, 703, "Expired JWT token");
            }

            // 6. Claims에서 memberUuid 추출
            String memberUuid = claims.get("memberUuid", String.class);
            if (memberUuid == null) {
                return sendError(exchange, 704, "memberUuid is missing");
            }

            // 7. 요청에 memberUuid를 헤더로 추가
            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header(HEADER_MEMBER_UUID, memberUuid)
                    .build();

            // 8. 수정된 요청으로 체인 계속 진행
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            // 예외 발생 시 999 에러와 함께 메시지 반환
            log.error("Token validation error: ", e);
            return sendError(exchange, 999, "Unexpected error: " + e.getMessage());
        }
    }

    // 토큰 만료 여부 확인
    private boolean isExpired(Date expiration) {
        return expiration.before(new Date());
    }

    // JWT에서 Claims(페이로드) 추출
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 에러 응답을 JSON 형태로 반환
    private Mono<Void> sendError(ServerWebExchange exchange, int statusCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            // 에러 메시지를 JSON 문자열로 변환
            String body = objectMapper.writeValueAsString(new ErrorResponse(statusCode, message));
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Flux.just(buffer));
        } catch (Exception e) {
            // JSON 변환 실패 시 빈 응답 반환
            return response.setComplete();
        }
    }

    // 에러 응답 객체 정의
    private record ErrorResponse(int statusCode, String message) {}
}