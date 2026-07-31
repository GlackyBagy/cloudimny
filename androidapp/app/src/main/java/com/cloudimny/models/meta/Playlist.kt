package com.cloudimny.models.meta

import java.util.UUID

data class Playlist(val id: UUID?, val songList: List<Track> = emptyList())
