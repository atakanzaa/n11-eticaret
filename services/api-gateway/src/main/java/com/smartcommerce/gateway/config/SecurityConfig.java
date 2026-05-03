package com.smartcommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security WebFlux at the gateway. We delegate authentication to our
 * own {@link com.smartcommerce.gateway.filter.JwtAuthenticationFilter}, so
 * Spring Security here just needs to:
 *   - permit every request (the JWT filter already populated SecurityContext
 *     for protected paths and 401'd unauthenticated calls to private routes),
 *   - disable CSRF (we're a stateless API; CSRF tokens make no sense),
 *   - disable HTTP Basic and form login defaults (irrelevant for an API).
 *
 * Without this bean, Spring Boot auto-config installs the default chain that
 * blocks every request with a 403 once a CSRF cookie hasn't been negotiated —
 * which is exactly what was breaking POST /api/payments/3ds-callback when
 * iyzico called back without any browser-style state.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(ex -> ex.anyExchange().permitAll())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }
}
