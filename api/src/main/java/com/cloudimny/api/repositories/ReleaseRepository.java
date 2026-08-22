package com.cloudimny.api.repositories;

import com.cloudimny.api.models.entities.Release;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ReleaseRepository extends R2dbcRepository<Release, UUID> {

    /**
     * Reserves the release before its cover is fetched. Returns the number of rows created, so a
     * caller that gets 0 knows a concurrent handler already owns this album and can skip the download.
     */
    @Modifying
    @Query("""
            INSERT INTO releases (id, type, release_date)
            VALUES (:id, :type, :releaseDate)
            ON CONFLICT (id) DO NOTHING
            """)
    Mono<Long> insertIfAbsent(@Param("id") UUID id,
                              @Param("type") String type,
                              @Param("releaseDate") LocalDate releaseDate);

    @Modifying
    @Query("""
            UPDATE releases
            SET cover_resolved = :resolved
            WHERE id = :id
            """)
    Mono<Void> setResolved(@Param("id") UUID id,
                           @Param("resolved") boolean resolved);

    @Query("""
                 SELECT *
                 FROM releases r
                 WHERE r.cover_resolved IS NULL AND
                       (NOW() AT TIME ZONE 'UTC') - r.timestamp > INTERVAL '10 minutes'
            """)
    Flux<Release> findAllUnresolved();

    @Query("""
             SELECT id
             FROM releases r
             WHERE (NOW() AT TIME ZONE 'UTC') - r.timestamp > INTERVAL '1 day'
                 AND NOT EXISTS (
                                   SELECT 1
                                   FROM tracks t
                                   WHERE t.release_id = r.id
                                )
            """)
    Flux<UUID> findAllUnused();
}
