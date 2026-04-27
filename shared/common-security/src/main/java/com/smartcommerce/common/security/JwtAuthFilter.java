package com.smartcommerce.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            var claims = jwtService.parseToken(token);
            if (!"ACCESS".equals(claims.get("type"))) {
                chain.doFilter(request, response);
                return;
            }

            var userId = claims.getSubject();
            @SuppressWarnings("unchecked")
            var roles = (List<String>) claims.get("roles");
            var authorities = roles == null ? List.<SimpleGrantedAuthority>of() :
                roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            request.setAttribute("X-User-Id", userId);
            request.setAttribute("X-User-Roles", String.join(",", roles == null ? List.of() : roles));
        } catch (Exception e) {
            log.debug("JWT processing failed: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getServletPath();
        return path.startsWith("/api/auth/") || path.startsWith("/actuator/") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }
}
