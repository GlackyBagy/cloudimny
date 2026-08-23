package com.cloudimny.api.services;

import com.cloudimny.api.models.covers.external.caarchive.ImagesResponse;
import com.cloudimny.api.models.covers.external.musicbrainz.MusicBrainzResponse;
import com.cloudimny.api.models.covers.external.musicbrainz.ReleaseResponse;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

@Service
public class CoverSourceService {
    private final WebClient coverArtClient;
    private final WebClient musicBrainzClient;
    private final RateLimiter musicBrainzRateLimiter;

    public CoverSourceService(@Value("${app.cover.coverartarchive.url}") String coverArtUrl,
                              @Value("${app.cover.musicbrainz.url}") String musicBrainz,
                              ReactorClientHttpConnector externalHttpConnector,
                              RateLimiter musicBrainzRateLimiter) {
        this.musicBrainzRateLimiter = musicBrainzRateLimiter;

        coverArtClient = WebClient.builder()
                .baseUrl(coverArtUrl)
                .clientConnector(externalHttpConnector)
                .build();

        musicBrainzClient = WebClient.builder()
                .baseUrl(musicBrainz)
                .clientConnector(externalHttpConnector)
                .build();
    }

    /**
     * Picks the release to look the cover up by: an official one if the search returned any,
     * otherwise the highest scored. Recordings come back ordered by relevance, and the top hit
     * is often a live or bootleg version, so status is worth more than position here.
     */
    public Optional<ReleaseResponse> pickRelease(MusicBrainzResponse response) {
        if (response.recordings() == null) {
            return Optional.empty();
        }
        return response.recordings().stream()
                .filter(recording -> recording.releases() != null)
                .flatMap(recording -> recording.releases().stream())
                .min(Comparator.comparingInt(release -> "Official".equals(release.status()) ? 0 : 1));
    }

    public Mono<MusicBrainzResponse> searchRecording(String title, String artistNickname) {
        var query = "artist:\"%s\" AND recording:\"%s\"".formatted(artistNickname, title);

        var request = musicBrainzClient.get()
                .uri(b -> b.path("/ws/2/recording")
                        .queryParam("query", query)
                        .queryParam("fmt", "json")
                        .build())
                .header(HttpHeaders.USER_AGENT, "cloudimny/0.0.1 ( cloudimny@example.com )");

        return request.retrieve().bodyToMono(MusicBrainzResponse.class)
                .transformDeferred(RateLimiterOperator.of(musicBrainzRateLimiter))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(WebClientRequestException.class::isInstance)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure()));
    }

    public Mono<ImagesResponse.Image> resolveCover(ReleaseResponse release) {
        return Mono.defer(() -> fetchCover("/release/{id}", release.id())
                .switchIfEmpty(Mono.defer(() ->
                        release.releaseGroup() != null ?
                                fetchCover("/release-group/{id}", release.releaseGroup().id())
                                : Mono.empty())
                )
        );
    }

    private Mono<ImagesResponse.Image> fetchCover(String path, UUID id) {
        return coverArtClient.get()
                .uri(path, id)
                .retrieve()
                .bodyToMono(ImagesResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, _ -> Mono.empty())
                .flatMap(response -> Mono.justOrEmpty(frontImage(response)))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(WebClientRequestException.class::isInstance)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure()));
    }


    private Optional<ImagesResponse.Image> frontImage(ImagesResponse response) {
        if (response.images() == null) {
            return Optional.empty();
        }
        return response.images().stream()
                .filter(ImagesResponse.Image::front)
                .findFirst();
    }
}
