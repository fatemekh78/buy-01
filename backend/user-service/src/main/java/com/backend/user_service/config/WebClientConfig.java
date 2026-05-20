package com.backend.user_service.config;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLException;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

/**
 * Network configuration for internal microservice-to-microservice
 * communication.
 * Provisions load-balanced HTTP clients via Eureka discovery.
 */
@Slf4j
@Configuration
public class WebClientConfig {

        /**
         * Constructs a reactive WebClient utilizing a custom, permissive SSL context.
         * WARNING: This configuration trusts all certificates (including self-signed)
         * and is strictly designed for local development and internal Docker networks.
         */
        @SuppressWarnings("null")
        @Bean
        @LoadBalanced
        public WebClient.Builder webClientBuilder() throws NoSuchAlgorithmException, KeyStoreException, SSLException {

                log.warn("INITIALIZING INSECURE WEB CLIENT: SSL Certificate validation is bypassed. " +
                                "Ensure this configuration is not deployed to external production networks.");

                SslContext sslContext = SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build();

                HttpClient httpClient = HttpClient.create().secure(spec -> spec.sslContext(sslContext));

                return WebClient.builder()
                                .clientConnector(new ReactorClientHttpConnector(httpClient));
        }

        @Bean
        @LoadBalanced
        public RestTemplate restTemplate() {
                return new RestTemplate();
        }

        /**
         * Internal trust manager implementation that blindly accepts all SSL
         * certificates.
         */
        private static class InsecureTrustManagerFactory {
                public static final io.netty.handler.ssl.util.InsecureTrustManagerFactory INSTANCE = (io.netty.handler.ssl.util.InsecureTrustManagerFactory) io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;
        }
}