package com.cloudimny.api.events;

import com.cloudimny.api.models.entities.Release;
import com.cloudimny.api.models.entities.Track;
import com.cloudimny.api.services.ReleaseService;
import com.cloudimny.api.services.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoverEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReleaseService releaseService;
    private final TrackService trackService;

    public void publishResolveTrackCoverEvent(UUID trackId) {
        applicationEventPublisher.publishEvent(
                new TrackCoverEvent(trackId)
        );
    }

    @Scheduled(fixedDelay = 900_000) // 15 mins
    protected void publishUnresolved() {
        releaseService.findUnresolved()
                .map(Release::id)
                .flatMap(trackService::findFirstByReleaseId)
                .map(Track::id)
                .doOnNext(this::publishResolveTrackCoverEvent)
                .subscribe();
    }
}
