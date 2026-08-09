package com.cloudimny.api.repositories;

import com.cloudimny.api.models.entities.Track;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface TrackRepository extends R2dbcRepository<Track, UUID> {

    /**
     * Written as a statement rather than through the entity because {@code Track} has no release_id
     * component — adding one would ripple through the mapper and every constructor call site.
     */
    @Modifying
    @Query("UPDATE tracks SET release_id = :releaseId WHERE id = :trackId")
    Mono<Long> attachRelease(@Param("trackId") UUID trackId, @Param("releaseId") UUID releaseId);
}
