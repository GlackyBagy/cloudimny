package com.cloudimny.api.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Profile("!dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyAuthFilter implements WebFilter {
    private final byte[] expectedSecret;

    public ApiKeyAuthFilter(@Value("${app.auth-secret}") String authSecret) {
        this.expectedSecret = authSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             @NonNull WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        byte[] provided = header != null
                ? header.getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        if (header == null || !MessageDigest.isEqual(provided, expectedSecret)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
