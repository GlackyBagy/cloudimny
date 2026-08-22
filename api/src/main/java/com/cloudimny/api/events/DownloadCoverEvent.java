package com.cloudimny.api.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
class DownloadCoverEvent extends ApplicationEvent {

    private final UUID trackId;

    public DownloadCoverEvent(UUID trackId) {
        super(trackId);
        this.trackId = trackId;
    }
}
