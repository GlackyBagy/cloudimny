package com.cloudimny.player

import com.cloudimny.models.meta.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackQueue {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val current: Track?
        get() = _tracks.value.getOrNull(_currentIndex.value)

    fun hasNext(): Boolean = _currentIndex.value != -1 && _currentIndex.value < _tracks.value.lastIndex

    fun hasPrevious(): Boolean = _currentIndex.value > 0

    fun set(tracks: List<Track>, startIndex: Int) {
        _tracks.value = tracks
        _currentIndex.value = startIndex.coerceIn(tracks.indices)
    }

    fun add(track: Track) {
        _tracks.value += track
        if (_currentIndex.value == -1) {
            _currentIndex.value = _tracks.value.lastIndex
        }
    }

    fun next(): Track? {
        if (!hasNext()) return null
        _currentIndex.value += 1
        return current
    }

    fun previous(): Track? {
        if (!hasPrevious()) return null
        _currentIndex.value -= 1
        return current
    }

    fun moveTo(track: Track): Boolean {
        val index = _tracks.value.indexOfFirst { it.id == track.id }
        if (index == -1) return false
        _currentIndex.value = index
        return true
    }

    fun clear() {
        _tracks.value = emptyList()
        _currentIndex.value = -1
    }
}
