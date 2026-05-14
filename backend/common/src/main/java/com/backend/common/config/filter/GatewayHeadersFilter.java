package com.backend.common.config.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Intercepts the HTTP request to establish security context from Gateway
     * headers.
     *
     * @param request  The incoming HTTP request.
     * @param response The outgoing HTTP response.
     * @param chain    The filter chain to continue execution.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException      If an I/O error occurs during filtering.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            String userId = request.getHeader("X-User-ID");
            String email = request.getHeader("X-User-Email");
            String rolesHeader = request.getHeader("X-User-Role");

            if (userId != null && email != null && rolesHeader != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                List<SimpleGrantedAuthority> authorities = extractAuthorities(rolesHeader);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Successfully authenticated user via Gateway headers: {}", email);
            }
        } catch (Exception e) {
            log.error("Error occurred while processing Gateway security headers: {}", e.getMessage(), e);
            // Depending on strictness, we might throw an exception here, but usually
            // in a filter we log and let it proceed as unauthenticated.
        }

        chain.doFilter(request, response);
    }

    /**
     * Parses the comma-separated roles header into a list of GrantedAuthorities.
     *
     * @param rolesHeader The comma-separated string of roles.
     * @return A list of SimpleGrantedAuthority objects prefixed with "ROLE_".
     * @throws IllegalArgumentException if the rolesHeader is null or empty.
     */
    private List<SimpleGrantedAuthority> extractAuthorities(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("Roles header cannot be empty");
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}