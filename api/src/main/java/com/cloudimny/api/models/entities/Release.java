package com.cloudimny.api.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A MusicBrainz release. The id is the MBID rather than a generated value, so rows are created
 * through {@code ReleaseRepository.insertIfAbsent}
 */
@Table("releases")
public record Release(@Id UUID id, String type, LocalDate releaseDate,
                      Instant timestamp, Boolean coverResolved) {
}
