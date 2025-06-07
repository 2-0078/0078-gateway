package com.pieceofcake.gateway.filter;

import com.pieceofcake.gateway.auth.JwtProvider;
import com.pieceofcake.gateway.common.exception.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtProvider jwtProvider;

    // 1. 화이트리스트 경로를 작성 (startsWith/equals/정규표현식 등 필요에 따라)
    private static final String[] WHITE_LIST = {
            "/auth-service/api/v1/login",
            "/auth-service/api/v1/signup",
            "/auth-service/api/v1/check-nickname",
            "/auth-service/api/v1/check-email",
            "/auth-service/api/v1/find-email",
            "/auth-service/api/v1/phone/send-code",
            "/auth-service/api/v1/phone/verify"
            // 추가적으로 인증 필요없는 경로들 여기에!
    };

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        super(Config.class);
        this.jwtProvider = jwtProvider;
    }

    public static class Config {
        // 차후 설정값 입력
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 2. 현재 요청 경로 확인
            String path = exchange.getRequest().getPath().toString();

            // 3. 화이트리스트 검사

            if (Arrays.stream(WHITE_LIST).anyMatch(path::startsWith) || isSwaggerPath(path) ) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return onError(exchange, BaseResponseStatus.WRONG_JWT_TOKEN);
            }

            String token = authorizationHeader.replace("Bearer ", "");

            if (!jwtProvider.validateToken(token)) {
                return onError(exchange, BaseResponseStatus.TOKEN_NOT_VALID);
            }

            // 토큰에서 uuid 추출
            String tokenUuid = jwtProvider.extractClaim(token, claims -> claims.get("uuid", String.class));
            // X-Member-Uuid 헤더에 추가
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Member-Uuid", tokenUuid)
                    .build();

            // 만약 X-Member-Uuid 헤더가 존재하고, 이걸 토큰의 추출값과 비교하고 싶다면, 위의 코드를 아래코드로 교체
//            String headerUuid = request.getHeaders().getFirst("X-Member-Uuid");
//            if (headerUuid == null || !headerUuid.equals(tokenUuid)) {
//                return onError(exchange, BaseResponseStatus.INVALID_ACCESS_TOKEN);
//            }

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(mutatedExchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, BaseResponseStatus status) {
        exchange.getResponse().setStatusCode(status.getHttpStatusCode());
        return exchange.getResponse().setComplete();
    }

    private boolean isSwaggerPath(String path) {
        return path.contains("/swagger") ||
                path.contains("/v3/api-docs") ||
                path.contains("/webjars");
    }

    private String resolveToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

}
