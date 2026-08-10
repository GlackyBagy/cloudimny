package com.cloudimny.api.services;

import com.cloudimny.api.events.CoverEventPublisher;
import lombok.RequiredArgsConstructor;
import com.cloudimny.api.models.dto.TrackDTO;
import com.cloudimny.api.models.entities.Track;
import com.cloudimny.api.models.mapping.TrackMapper;
import com.cloudimny.api.models.payload.TrackPayload;
import com.cloudimny.api.repositories.TrackRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackService {
    private final TrackRepository repository;
    private final ArtistService artistService;
    private final TrackMapper trackMapper;
    private final CoverEventPublisher coverEventPublisher;

    public Mono<Track> create(TrackPayload payload) {
        return artistService.createFromNickname(payload.artist().nickname())
                .map(artist -> trackMapper.toTrack(payload, null, artist.id()))
                .map(track -> new Track(track.id(), track.title().trim(), track.artistId(), track.storageKey(), Instant.now(), null))
                .flatMap(repository::save)
                .doOnNext(track -> coverEventPublisher.publishResolveTrackCoverEvent(track.id()));
    }

    public Mono<Track> findById(UUID id) {
        return repository.findById(id);
    }

    public Flux<Track> findAll() {
        return repository.findAll();
    }

    public Mono<Track> findFirstByReleaseId(UUID releaseId) {
        return repository.findFirstByReleaseId(releaseId);
    }

    public Mono<Track> update(UUID id, TrackPayload payload) {
        return repository.findById(id)
                .flatMap(existing -> artistService.createFromNickname(payload.artist().nickname())
                        .map(artist -> new Track(
                                existing.id(),
                                payload.title(),
                                artist.id(),
                                existing.storageKey(),
                                existing.timestamp(),
                                existing.releaseId()
                        )))
                .flatMap(repository::save);
    }

    public Mono<TrackDTO> toDTO(Track track) {
        return artistService.findById(track.artistId())
                .map(artist -> trackMapper.toDTO(track, artist));
    }

    public Mono<Long> attachRelease(UUID trackId, UUID releaseId) {
        return repository.attachRelease(trackId, releaseId);
    }

    public Mono<Track> attachStorageKey(Track track, String storageKey) {
        return repository.save(new Track(track.id(), track.title(), track.artistId(), storageKey, track.timestamp(), track.releaseId()));
    }
}
