package com.backend.api_gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global centralized exception handler for the reactive Spring Cloud Gateway.
 * Intercepts all routing, filter, and security exceptions, formatting them into
 * a standardized JSON response contract identical to the backend microservices.
 */
@Slf4j
@Component
@Order(-2) // Ensures this handler runs before Spring's DefaultErrorWebExceptionHandler
public class WebFluxGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public WebFluxGlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred in the API Gateway.";

        // 1. Handle Custom Gateway Exceptions
        if (ex instanceof CustomException c) {
            status = c.getStatus();
            message = c.getMessage();
            log.warn("Gateway rule exception ({}): {}", status, message);
        } 
        // 2. Handle Spring WebFlux/Gateway Native Exceptions (e.g., 404 Not Found, 413 Payload Too Large)
        else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason();
            log.warn("Gateway routing/filter exception ({}): {}", status, message);
        } 
        // 3. Catch-All for Critical Server Crashes
        else {
            log.error("CRITICAL: Unhandled API Gateway exception for URI {}", exchange.getRequest().getURI(), ex);
        }

        return writeResponse(exchange, status, message);
    }

    /**
     * Constructs the standardized JSON error payload and writes it to the reactive response stream.
     *
     * @param exchange The current web exchange.
     * @param status   The resolved HTTP status code.
     * @param message  The client-facing error message.
     * @return Mono<Void> indicating the response has been fully written.
     */
    @SuppressWarnings("null")
    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            return Mono.error(e);
        }
    }
}