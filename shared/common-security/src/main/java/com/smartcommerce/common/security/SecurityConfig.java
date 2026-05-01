package com.smartcommerce.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
        @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        return new JwtService(secret, accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/products/**",
                    "/api/offers/**",
                    "/api/categories/**",
                    "/api/brands/**",
                    "/api/reviews/*",
                    "/api/cart/internal/**",
                    "/api/users/internal/**",
                    "/api/sellers/by-user/**",
                    "/api/sellers/*",
                    "/api/inventory/reserve",
                    "/api/inventory/orders/**",
                    "/api/inventory/internal/**",
                    "/api/orders/internal/**",
                    "/api/payments/iyzico/**",
                    "/api/payments/internal/**",
                    "/api/search/**",
                    "/api/catalog/**",
                    "/api/coupons/validate",
                    "/api/coupons/internal/**",
                    "/api/campaigns/active-by-offer/**",
                    "/api/shipments/track/**",
                    "/api/shipments/internal/**",
                    "/api/recommendations/popular",
                    "/api/recommendations/products/**",
                    "/api/recommendations/internal/**",
                    "/api/fraud/check",
                    "/api/fraud/internal/**",
                    "/api/returns/internal/**",
                    "/api/ai/internal/**",
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
