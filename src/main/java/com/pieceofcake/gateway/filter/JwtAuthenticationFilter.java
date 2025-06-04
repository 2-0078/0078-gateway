package com.pieceofcake.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pieceofcake.gateway.auth.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GatewayFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_MEMBER_UUID = "X-Member-Uuid";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("JWT Authorization Filter");

        try {
            List<String> authorizationHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || authorizationHeader.isEmpty()) {
                return sendError(exchange, 701, "Authorization header is missing");
            }

            String bearerToken = authorizationHeader.stream()
                    .filter(header -> header.startsWith(BEARER_PREFIX))
                    .findFirst()
                    .orElse(null);

            if (bearerToken == null) {
                return sendError(exchange, 702, "Bearer token is missing");
            }

            String token = bearerToken.substring(BEARER_PREFIX.length());

            if (!jwtProvider.validateToken(token)) {
                return sendError(exchange, 703, "Invalid or expired JWT token");
            }

            Claims claims = jwtProvider.parseClaims(token);
            String memberUuid = claims.get("memberUuid", String.class);
            if (memberUuid == null) {
                return sendError(exchange, 704, "memberUuid is missing in JWT");
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header(HEADER_MEMBER_UUID, memberUuid)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Unexpected JWT error", e);
            return sendError(exchange, 999, "Unexpected error: " + e.getMessage());
        }
    }

    private Mono<Void> sendError(ServerWebExchange exchange, int statusCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            String body = objectMapper.writeValueAsString(new ErrorResponse(statusCode, message));
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Flux.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    private record ErrorResponse(int statusCode, String message) {}
}