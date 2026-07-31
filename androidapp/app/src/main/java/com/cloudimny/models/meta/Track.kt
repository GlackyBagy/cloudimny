package com.cloudimny.models.meta

import java.util.UUID

data class Track(
    val id: UUID?,
    val title: String?,
    val artist: Artist?
)