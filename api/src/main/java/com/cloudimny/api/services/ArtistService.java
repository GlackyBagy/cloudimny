package com.cloudimny.api.services;

import lombok.RequiredArgsConstructor;
import com.cloudimny.api.models.entities.Artist;
import com.cloudimny.api.models.mapping.ArtistMapper;
import com.cloudimny.api.models.payload.ArtistPayload;
import com.cloudimny.api.repositories.ArtistRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistService {
    private final ArtistRepository repository;
    private final ArtistMapper artistMapper;

    public Mono<Artist> createFromNickname(String nickname) {
        return repository.findByNicknameIgnoreCase(nickname.trim())
                .switchIfEmpty(repository.save(new Artist(null, nickname.trim())));
    }

    public Mono<Artist> findById(UUID id) {
        return repository.findById(id);
    }

    public Mono<Artist> findByNickname(String nickname) {
        return repository.findByNicknameIgnoreCase(nickname);
    }

    public Mono<Artist> update(UUID id, ArtistPayload payload) {
        return repository.findById(id)
                .map(existing -> artistMapper.toArtist(payload, existing.id()))
                .flatMap(repository::save);
    }

    public Flux<Artist> findAll() {
        return repository.findAll();
    }

}
