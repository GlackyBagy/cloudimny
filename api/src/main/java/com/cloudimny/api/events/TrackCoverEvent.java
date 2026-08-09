package com.cloudimny.api.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class TrackCoverEvent extends ApplicationEvent {

    private final UUID trackId;

    public TrackCoverEvent(UUID trackId) {
        super(trackId);
        this.trackId = trackId;
    }
}
