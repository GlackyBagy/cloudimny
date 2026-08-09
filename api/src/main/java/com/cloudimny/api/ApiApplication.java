package com.cloudimny.api;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@SpringBootApplication
@EnableR2dbcRepositories
public class ApiApplication {

    static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    public DataBufferFactory dataBufferFactory() {
        return DefaultDataBufferFactory.sharedInstance;
    }

    /**
     * Idle connections are dropped well before the remote hosts close them on their side. The default
     * pool keeps them forever, hands a dead socket to the next request, and that request fails with
     * "Connection prematurely closed BEFORE response" — always the first one after a pause.
     */
    @Bean
    public ConnectionProvider externalHttpPool() {
        return ConnectionProvider.builder("external-http")
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofMinutes(5))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Cover Art Archive answers 307 to archive.org, and Reactor Netty does not follow redirects by
     * default. The response timeout covers MusicBrainz throttling, which shows up as replies taking
     * twenty seconds rather than as an error.

     * MusicBrainz asks clients to stay at about one request per second. The timeout is what a caller
     * is willing to wait in the queue: uploading a whole album queues one request per track, so it
     * has to cover the longest expected backlog rather than a single hop.
     */
    @Bean
    public RateLimiter musicBrainzRateLimiter() {
        return RateLimiter.of("musicbrainz", RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMinutes(2))
                .build());
    }

    @Bean
    public ReactorClientHttpConnector externalHttpConnector(ConnectionProvider externalHttpPool) {
        return new ReactorClientHttpConnector(
                HttpClient.create(externalHttpPool)
                        .followRedirect(true)
                        .responseTimeout(Duration.ofSeconds(30)));
    }
}
