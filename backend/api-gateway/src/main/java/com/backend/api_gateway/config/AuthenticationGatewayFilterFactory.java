package com.backend.api_gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.backend.api_gateway.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Custom Gateway Filter responsible for intercepting incoming requests,
 * extracting and validating the JWT from cookies, and propagating user identity
 * downstream via secure HTTP headers.
 */
@Slf4j
@Component
public class AuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> authenticateAndForward(exchange, chain);
    }

    /**
     * Executes the authentication logic on the reactive stream.
     *
     * @param exchange The current server web exchange containing request/response
     *                 context.
     * @param chain    The gateway filter chain to delegate to upon success.
     * @return A Mono representing the reactive completion of the filter.
     */
    private Mono<Void> authenticateAndForward(ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        var jwtCookie = request.getCookies().getFirst("jwt");
        String token = jwtCookie != null ? jwtCookie.getValue() : null;

        if (token == null) {
            log.warn("Authentication denied: Missing JWT cookie for URI {}", request.getURI().getPath());
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token"));
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("Authentication denied: Invalid or expired JWT for URI {}", request.getURI().getPath());
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token"));
        }

        // Extract validated claims
        String email = jwtUtil.getUsernameFromToken(token);
        String userId = jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", String.class));
        String role = jwtUtil.getClaimFromToken(token, claims -> claims.get("role", String.class));

        log.debug("Successfully authenticated user {} with role {}", email, role);

        // Mutate request to inject trusted downstream headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Email", email)
                .header("X-User-ID", userId)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Configuration class required by AbstractGatewayFilterFactory.
     */
    public static class Config {
        // Can be expanded if route-specific configurations are needed in the future
    }
}