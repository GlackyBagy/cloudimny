package com.cloudimny.api.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("artists")
public record Artist(@Id UUID id, String nickname) {
}
