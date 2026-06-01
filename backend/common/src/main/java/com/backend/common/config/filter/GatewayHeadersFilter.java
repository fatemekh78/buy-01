package com.backend.common.config.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Security filter that intercepts requests to extract user identity and roles
 * passed down by the API Gateway and establishes a Spring Security Context.
 */
@Slf4j
@Component
public class GatewayHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-ID");
        String email = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Role");

        // Only set auth context if ALL three headers are present and non-blank
        if (userId != null && !userId.isBlank() &&
                email != null && !email.isBlank() &&
                rolesHeader != null && !rolesHeader.isBlank() &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // Inline parsing — same as old version, no throw risk
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty()) // guard against "ROLE1,,ROLE2"
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, null,
                    authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Authenticated via Gateway headers: {} with roles {}", email, authorities);
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Auth endpoints are public — no headers present yet
        return request.getRequestURI().startsWith("/api/auth/");
    }
}