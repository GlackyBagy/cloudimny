package com.cloudimny.api.services;

import lombok.RequiredArgsConstructor;
import com.cloudimny.api.models.entities.Playlist;
import com.cloudimny.api.repositories.PlaylistRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository repository;

    public Mono<Playlist> create() {
        return repository.save(new Playlist(null));
    }

    public Mono<Playlist> findById(UUID id) {
        return repository.findById(id);
    }
}
