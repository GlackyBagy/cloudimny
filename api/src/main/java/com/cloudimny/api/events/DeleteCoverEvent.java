package com.cloudimny.api.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
class DeleteCoverEvent extends ApplicationEvent {
    private final UUID releaseId;

    public DeleteCoverEvent(UUID releaseId) {
        super(releaseId);
        this.releaseId = releaseId;
    }
}
