package com.cloudimny.api.services;

import lombok.RequiredArgsConstructor;
import com.cloudimny.api.models.dto.PlaylistDTO;
import com.cloudimny.api.models.entities.Playlist;
import com.cloudimny.api.models.entities.PlaylistTrack;
import com.cloudimny.api.repositories.PlaylistRepository;
import com.cloudimny.api.repositories.PlaylistTrackRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository repository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final TrackService trackService;

    public Mono<Playlist> create(String name, List<UUID> trackIds) {
        return repository.save(new Playlist(null, name))
                .flatMap(playlist -> Flux.fromIterable(trackIds)
                        .index((position, trackId) -> new PlaylistTrack(null, playlist.id(), trackId, position.intValue()))
                        .flatMap(playlistTrackRepository::save)
                        .then(Mono.just(playlist)));
    }

    public Mono<Playlist> findById(UUID id) {
        return repository.findById(id);
    }

    public Flux<Playlist> findAll() {
        return repository.findAll();
    }

    public Mono<PlaylistDTO> toDTO(Playlist playlist) {
        return playlistTrackRepository.findByPlaylistIdOrderByPositionAsc(playlist.id())
                .concatMap(playlistTrack -> trackService.findById(playlistTrack.trackId()))
                .concatMap(trackService::toDTO)
                .collectList()
                .map(tracks -> new PlaylistDTO(playlist.id(), playlist.name(), tracks));
    }
}
