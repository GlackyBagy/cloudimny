package com.cloudimny.api.repositories;

import com.cloudimny.api.models.entities.PlaylistTrack;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface PlaylistTrackRepository extends R2dbcRepository<PlaylistTrack, UUID> {
    Flux<PlaylistTrack> findByPlaylistIdOrderByPositionAsc(UUID playlistId);
}
