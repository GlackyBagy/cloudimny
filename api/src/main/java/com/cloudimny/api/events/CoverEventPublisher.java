package com.cloudimny.api.events;

import com.cloudimny.api.models.entities.Release;
import com.cloudimny.api.models.entities.Track;
import com.cloudimny.api.services.ReleaseService;
import com.cloudimny.api.services.TrackService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CoverEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReleaseService releaseService;
    private final TrackService trackService;

    public CoverEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                               ReleaseService releaseService,
                               @Lazy TrackService trackService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.releaseService = releaseService;
        this.trackService = trackService;
    }

    public void publishDownloadCoverEvent(UUID trackId) {
        applicationEventPublisher.publishEvent(
                new DownloadCoverEvent(trackId)
        );
    }

    public void publishDeleteCoverEvent(UUID releaseId) {
        applicationEventPublisher.publishEvent(
                new DeleteCoverEvent(releaseId)
        );
    }

    @Scheduled(fixedDelay = 900_000) // 15 mins
    protected void publishUnresolved() {
        releaseService.findUnresolved()
                .map(Release::id)
                .flatMap(trackService::findFirstByReleaseId)
                .map(Track::id)
                .doOnNext(this::publishDownloadCoverEvent)
                .subscribe();
    }

    @Scheduled(fixedDelay = 900_000) // 15 mins
    protected void publishUnused() {
        releaseService.findAllUnusedIds()
                .doOnNext(this::publishDeleteCoverEvent)
                .subscribe();
    }
}
