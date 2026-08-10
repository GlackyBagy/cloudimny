package com.cloudimny.api.services;

import com.cloudimny.api.models.covers.external.musicbrainz.ReleaseResponse;
import com.cloudimny.api.models.entities.Release;
import com.cloudimny.api.repositories.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseService {
    private static final String DEFAULT_TYPE = "Album";

    private final ReleaseRepository repository;

    /**
     * Stores the release unless another handler already did. Emits {@code true} only for the caller
     * that created the row — that one owns fetching the cover, everyone else reuses what it stores.
     */
    public Mono<Boolean> claim(ReleaseResponse release) {
        return repository.insertIfAbsent(release.id(), typeOf(release), dateOf(release))
                .map(rows -> rows > 0);
    }

    public Mono<Void> setCoverResolved(UUID id, boolean resolved) {
        return repository.setResolved(id, resolved);
    }

    public Mono<Release> findById(UUID id) {
        return repository.findById(id);
    }

    public Flux<Release> findUnresolved() {
        return repository.findAllUnresolved();
    }

    /**
     * Covers are stored under the release MBID, so every track of an album resolves to one object.
     */
    public String coverKey(UUID releaseId) {
        return releaseId.toString();
    }

    private String typeOf(ReleaseResponse release) {
        if (release.releaseGroup() == null || release.releaseGroup().primaryType() == null) {
            return DEFAULT_TYPE;
        }
        return release.releaseGroup().primaryType();
    }

    /**
     * MusicBrainz dates are partial as often as not — "1997" and "1997-08" are as valid as a full
     * date, and a release may carry none at all.
     */
    private LocalDate dateOf(ReleaseResponse release) {
        String date = release.date();
        if (date == null || date.isBlank()) {
            return null;
        }

        try {
            return switch (date.length()) {
                case 4 -> LocalDate.of(Integer.parseInt(date), 1, 1);
                case 7 -> YearMonth.parse(date).atDay(1);
                default -> LocalDate.parse(date);
            };
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
    }
}
