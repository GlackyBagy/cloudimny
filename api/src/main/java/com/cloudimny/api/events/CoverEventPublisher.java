package com.cloudimny.api.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoverEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishResolveTrackCoverEvent(UUID trackId) {
        applicationEventPublisher.publishEvent(
                new TrackCoverEvent(trackId)
        );
    }
}
