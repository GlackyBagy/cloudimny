package com.cloudimny.api.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("tracks")
public record Track(@Id UUID id, String title, UUID artistId, String storageKey, Instant timestamp) {
}
