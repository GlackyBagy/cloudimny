package com.cloudimny.api.repositories;

import com.cloudimny.api.models.entities.Artist;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ArtistRepository extends R2dbcRepository<Artist, UUID> {
    Mono<Artist> findByNicknameIgnoreCase(String nickname);
}
