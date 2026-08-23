package com.cloudimny.api.events;

import com.cloudimny.api.models.covers.external.caarchive.ImagesResponse;
import com.cloudimny.api.models.covers.external.musicbrainz.ReleaseResponse;
import com.cloudimny.api.services.ArtistService;
import com.cloudimny.api.services.CoverSourceService;
import com.cloudimny.api.services.ReleaseService;
import com.cloudimny.api.services.StorageService;
import com.cloudimny.api.services.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class CoverEventHandler {

    private static final String THUMBNAIL_SIZE = "large";

    private final StorageService storageService;
    private final CoverSourceService coverService;
    private final TrackService trackService;
    private final ArtistService artistService;
    private final ReleaseService releaseService;

    @EventListener
    protected Mono<Void> handleDelete(DeleteCoverEvent event) {
        String coverKey = releaseService.coverKey(event.getReleaseId());

        return storageService.deleteCover(coverKey)
                .then(releaseService.deleteById(event.getReleaseId()))
                .doOnError(error -> log.error("Cover cleanup failed for release {}", event.getReleaseId(), error));
    }

    @EventListener
    protected Mono<Void> handleDownload(DownloadCoverEvent event) {
        log.info("Cover event received for track {}", event.getTrackId());

        return resolveRelease(event.getTrackId())
                .doOnSubscribe(_ -> log.info("Cover chain subscribed for track {}", event.getTrackId()))
                .doOnNext(release -> log.info("Picked release {} for track {}", release.id(), event.getTrackId()))
                .flatMap(release -> trackService.attachRelease(event.getTrackId(), release.id())
                        .then(storeCoverIfUnclaimed(release)))
                .doOnError(error -> log.error("Cover resolution failed for track {}", event.getTrackId(), error))
                .then();
    }

    private Mono<ReleaseResponse> resolveRelease(UUID trackId) {
        return trackService.findById(trackId)
                .doOnNext(track -> log.info("Searching cover for '{}'", track.title()))
                .flatMap(track -> artistService.findById(track.artistId())
                        .flatMap(artist -> coverService.searchRecording(track.title(), artist.nickname())))
                .flatMap(response -> Mono.justOrEmpty(coverService.pickRelease(response)));
    }

    /**
     * Only the handler that created the release row fetches the cover. Every other track of the same
     * album loses the race, skips both Cover Art Archive and the download, and just links itself.
     */
    private Mono<Void> storeCoverIfUnclaimed(ReleaseResponse release) {
        return releaseService.claim(release)
                .filter(Boolean::booleanValue)
                .flatMap(_ -> storeCover(release));
    }

    /**
     * Records the outcome either way: a release whose cover simply does not exist anywhere is marked
     * resolved too, so it is not searched for again on the next track of the same album.
     */
    private Mono<Void> storeCover(ReleaseResponse release) {
        return coverService.resolveCover(release)
                .flatMap(image -> storageService.downloadCover(
                                releaseService.coverKey(release.id()), thumbnailOf(image))
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .flatMap(found -> releaseService.setCoverResolved(release.id(), found));
    }

    /**
     * The full-size scan runs to several megabytes, past the codec buffer limit, so the thumbnail is
     * both smaller and the only size that reliably fits.
     */
    private URI thumbnailOf(ImagesResponse.Image image) {
        if (image.thumbnails() == null) {
            return image.image();
        }
        return image.thumbnails().getOrDefault(THUMBNAIL_SIZE, image.image());
    }
}
