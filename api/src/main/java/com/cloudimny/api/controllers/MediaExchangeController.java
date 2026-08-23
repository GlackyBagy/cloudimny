package com.cloudimny.api.controllers;

import com.cloudimny.api.models.dto.TrackDTO;
import com.cloudimny.api.models.payload.TrackPayload;
import com.cloudimny.api.services.ReleaseService;
import com.cloudimny.api.services.StorageService;
import com.cloudimny.api.services.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MediaExchangeController {
    private final StorageService storageService;
    private final TrackService trackService;
    private final ReleaseService releaseService;

    @PostMapping("/upload")
    public Mono<TrackDTO> uploadTrack(@RequestPart("file") FilePart file,
                                      @RequestPart("meta") TrackPayload payload) {
        log.info("Uploading a track: {}", payload);
        return trackService.create(payload)
                .flatMap(track -> storageService.uploadTrack(track.id().toString(), file)
                        .then(trackService.attachStorageKey(track, track.id().toString())))
                .flatMap(trackService::toDTO);
    }

    @GetMapping("/streaming/{id}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> trackById(@PathVariable UUID id,
                                                            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        return trackService.findById(id)
                .flatMap(track -> storageService.loadTrack(track.storageKey(), range));
    }

    @GetMapping("/cover")
    public Mono<ResponseEntity<Flux<DataBuffer>>> coverByTrackId(@RequestParam UUID trackId) {
        return trackService.findById(trackId)
                .flatMap(track -> Mono.justOrEmpty(track.releaseId()))
                .map(releaseService::coverKey)
                .flatMap(storageService::loadCover)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
