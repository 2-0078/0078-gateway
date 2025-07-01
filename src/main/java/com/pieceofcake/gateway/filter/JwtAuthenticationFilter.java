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
import org.springframework.util.AntPathMatcher;


import java.util.Arrays;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtProvider jwtProvider;
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String[] WHITE_LIST = {
            "/auth-service/api/v1/signup",
            "/auth-service/api/v1/reset-password",
            "/auth-service/api/v1/logout",
            "/auth-service/api/v1/login",
            "/auth-service/api/v1/phone/verify",
            "/auth-service/api/v1/phone/send-code",

            "/member-service/api/v1/profile-image",
            "/member-service/api/v1/find-email",
            "/member-service/api/v1/check-nickname",
            "/member-service/api/v1/check-email",

            "/piece-service/api/v1/piece/product",
            "/piece-service/api/v1/piece/product/uuid-list",
            "/piece-service/api/v1/piece/product/market-price/{pieceProductUuid}",
            "/piece-service/api/v1/piece/product/{pieceProductUuid}",
            "/piece-service/api/v1/piece",
            "/piece-service/api/v1/piece/delete-all/{pieceProductUuid}",
            "/piece-service/api/v1/piece/owned/{pieceProductUuid}/list",

            "/funding-service/api/v1/funding/{fundingUuid}",
            "/funding-service/api/v1/funding/list",
            "/funding-service/api/v1/funding/all",
            "/funding-service/api/v1/funding/all/{status}",
            "/funding-service/api/v1/participation/remain/{fundingUuid}",

            "/reply-service/api/v1/reply/child/{parentReplyUuid}",
            "/reply-service/api/v1/reply/list/{boardType}/{boardUuid}",
            "/reply-service/api/v1/reply/community/{replyUuid}",

            "/payment-service/api/v1/money/with-member-uuid",
            "/payment-service/api/v1/brandpay/callback",

            "/auction-service/api/v1/vote",
            "/auction-service/api/v1/bid/list",
            "/auction-service/api/v1/auction/{auctionUuid}",
            "/auction-service/api/v1/auction",
            "/auction-service/api/v1/auction/sse/price-updates/{auctionUuid}",
            "/auction-service/api/v1/auction/list",
            "/auction-service/api/v1/auction/highest-price/{auctionUuid}",

            "/batch-service/api/v1/piece/graph/yearly/{pieceProductUuid}",
            "/batch-service/api/v1/piece/graph/real-time/{pieceProductUuid}",
            "/batch-service/api/v1/piece/graph/monthly/{pieceProductUuid}",
            "/batch-service/api/v1/piece/graph/daily/{pieceProductUuid}",
            "/batch-service/api/v1/best",

            "/board-service/api/v1/price",
            "/board-service/api/v1/board/notice",
            "/board-service/api/v1/board/notice/{boardUuid}",
            "/board-service/api/v1/board/faq",
            "/board-service/api/v1/board/faq/{boardUuid}",
            "/board-service/api/v1/board/event",
            "/board-service/api/v1/board/event/{boardUuid}",
            "/board-service/api/v1/board/community"
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

            if (Arrays.stream(WHITE_LIST).anyMatch(pattern -> pathMatcher.match(pattern, path)) || isSwaggerPath(path)) {
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

//            // 토큰에서 uuid 추출
//            String tokenUuid = jwtProvider.extractClaim(token, claims -> claims.get("uuid", String.class));
//            // X-Member-Uuid 헤더에 추가
//            ServerHttpRequest mutatedRequest = request.mutate()
//                    .header("X-Member-Uuid", tokenUuid)
//                    .build();

            // ✅ JwtProvider 내부 로직에 따라 memberUuid를 추출
            String memberUuid;
            String memberRole;
            try {
                memberUuid = jwtProvider.getMemberUuid(token);
                memberRole = jwtProvider.getMemberRole(token); // ✅ ROLE 추출
            } catch (Exception e) {
                log.warn("JWT에서 memberUuid 추출 실패: {}", e.getMessage());
                return onError(exchange, BaseResponseStatus.NO_ACCESS_AUTHORITY);
            }

            // 요청에 X-Member-Uuid 헤더 추가
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Member-Uuid", memberUuid)
                    .header("X-Member-Role", memberRole) // ✅ 역할 추가
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
        return path.contains("/swagger-ui") ||
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
