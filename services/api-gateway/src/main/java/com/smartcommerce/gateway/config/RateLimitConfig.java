package com.smartcommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Rate limiter key strategy: prefer X-Forwarded-For (when behind a proxy/CDN)
 * else fall back to the remote address. Used by /api/auth/** to slow down
 * brute-force attempts and credential-stuffing without locking legitimate users
 * behind shared NAT.
 *
 * Replenish rate / burst capacity are tuned in application.yml per route.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return Mono.just(xff.split(",")[0].trim());
            }
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            return Mono.just(remote == null ? "unknown" : remote.getAddress().getHostAddress());
        };
    }
}
