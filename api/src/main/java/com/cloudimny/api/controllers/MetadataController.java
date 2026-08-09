package com.cloudimny.api.controllers;

import com.cloudimny.api.models.dto.ArtistDTO;
import com.cloudimny.api.models.dto.PlaylistDTO;
import com.cloudimny.api.models.dto.TrackDTO;
import com.cloudimny.api.models.entities.Track;
import com.cloudimny.api.models.mapping.ArtistMapper;
import com.cloudimny.api.models.mapping.TrackMapper;
import com.cloudimny.api.models.payload.ArtistPayload;
import com.cloudimny.api.models.payload.PlaylistPayload;
import com.cloudimny.api.models.payload.TrackPayload;
import com.cloudimny.api.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MetadataController {
    private final ArtistService artistService;
    private final ArtistMapper artistMapper;
    private final TrackService trackService;
    private final PlaylistService playlistService;
    private final TrackMapper trackMapper;
    private final CoverSourceService coverSourceService;
    private final StorageService storageService;

    @GetMapping("/test")
    public Mono<Void> test(@RequestParam String track,
                           @RequestParam String artist) {
        return coverSourceService.searchRecording(track, artist)
                .flatMap(response -> Mono.justOrEmpty(coverSourceService.pickRelease(response)))
                .flatMap(release -> coverSourceService.resolveCover(release)
                        .flatMap(image -> storageService.downloadCover(
                                release.id().toString(), image.thumbnails().get("large"))));
    }

    @PostMapping("/artist")
    public Mono<Void> createArtist(@RequestBody ArtistPayload payload) {
        return artistService.createFromNickname(payload.nickname()).then();
    }

    @PutMapping("/artist/{id}")
    public Mono<ArtistDTO> updateArtist(@PathVariable UUID id, @RequestBody ArtistPayload payload) {
        return artistService.update(id, payload).map(artistMapper::toDTO);
    }

    @PutMapping("/track/{id}")
    public Mono<TrackDTO> updateTrack(@PathVariable UUID id, @RequestBody TrackPayload payload) {
        return trackService.update(id, payload).flatMap(trackService::toDTO);
    }


    @PostMapping("/playlist")
    public Mono<PlaylistDTO> createPlaylist(@RequestBody PlaylistPayload payload) {
        return playlistService.create(payload.name(), payload.trackIds())
                .flatMap(playlistService::toDTO);
    }

    @GetMapping("/playlist")
    public Flux<PlaylistDTO> allPlaylists() {
        return playlistService.findAll().concatMap(playlistService::toDTO);
    }

    @GetMapping("/playlist/{id}")
    public Mono<PlaylistDTO> getPlaylist(@PathVariable UUID id) {
        return playlistService.findById(id).flatMap(playlistService::toDTO);
    }

    @GetMapping("/track")
    public Flux<TrackDTO> allTracks() {
        return trackService.findAll()
                .groupBy(Track::artistId)
                .flatMap(grouped -> artistService.findById(grouped.key())
                        .flatMapMany(artist -> grouped.map(x -> trackMapper.toDTO(x, artist))));
    }
}
