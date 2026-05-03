package com.smartcommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
        // Auth
        "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
        // Storefront — public browsing (no login required)
        "/api/categories", "/api/products", "/api/search",
        "/api/brands",
        // Review read (public)
        "/api/reviews",
        // Campaign (active-by-offer is public for product detail badge)
        "/api/campaigns/active-by-offer",
        // Payment provider 3DS callback — iyzico can't include a JWT.
        // Real endpoint is /api/payments/iyzico/callback (see IyzicoWebhookController).
        "/api/payments/iyzico/callback",
        // Infrastructure
        "/actuator", "/swagger-ui", "/v3/api-docs"
    );

    private final SecretKey secretKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var path = exchange.getRequest().getPath().toString();
        var method = exchange.getRequest().getMethod();
        log.info("JWT filter — method={} path={} isPublic={}", method, path, isPublicPath(path));
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        var authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("JWT filter — REJECTED (no Bearer token) path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        var token = authHeader.substring(7);
        try {
            var claims = parseToken(token);
            if (!"ACCESS".equals(claims.get("type"))) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            var userId = claims.getSubject();
            @SuppressWarnings("unchecked")
            var roles = (List<String>) claims.get("roles");
            var rolesStr = roles == null ? "" : String.join(",", roles);

            var mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", rolesStr)
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
