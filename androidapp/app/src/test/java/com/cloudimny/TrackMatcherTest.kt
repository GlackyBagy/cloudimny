package com.cloudimny

import com.cloudimny.models.matching.TrackMatcher
import com.cloudimny.models.meta.Artist
import com.cloudimny.models.meta.Track
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class TrackMatcherTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "Bohemian Rhapsody (Remastered 2011)  | bohemian rhapsody",
            "Smells Like Teen Spirit [Official Video] | smells like teen spirit",
            "Numb / Encore                        | numb encore",
            "Café del Mar                         | cafe del mar",
            "Simon & Garfunkel                    | simon garfunkel",
            "Track Name - Radio Edit              | track name",
            "Song feat. Someone Else              | song",
            "  Extra   Spaces  Here               | extra spaces here"
        ],
        delimiter = '|'
    )
    fun normalizes_away_decorations(raw: String, expected: String) {
        assertEquals(expected.trim(), TrackMatcher.normalize(raw.trim()))
    }

    /** Название целиком в скобках — снимать их можно, но выбрасывать содержимое нельзя. */
    @Test
    fun keeps_content_when_title_is_entirely_bracketed() {
        assertEquals("dont fear the reaper", TrackMatcher.normalize("(Don't Fear) The Reaper"))
    }

    @Test
    fun keeps_words_when_every_word_looks_like_noise() {
        assertTrue(TrackMatcher.normalize("Live").isNotEmpty())
    }

    @Test
    fun similarity_is_one_for_identical_and_zero_for_empty() {
        assertEquals(1.0, TrackMatcher.similarity("abc", "abc"))
        assertEquals(0.0, TrackMatcher.similarity("abc", ""))
    }

    @Test
    fun similarity_survives_a_typo() {
        assertTrue(TrackMatcher.similarity("bohemian rhapsody", "bohemain rhapsody") > 0.7)
    }

    @Test
    fun matches_exactly_after_normalization() {
        val matched = TrackMatcher.match(
            "Bohemian Rhapsody (Remastered 2011)", "Queen", LIBRARY
        )
        assertEquals("Bohemian Rhapsody", matched?.title)
    }

    @Test
    fun matches_through_a_typo_in_the_title() {
        val matched = TrackMatcher.match("Bohemain Rhapsody", "Queen", LIBRARY)
        assertEquals("Bohemian Rhapsody", matched?.title)
    }

    @Test
    fun matches_when_source_puts_artist_and_title_in_one_field() {
        val matched = TrackMatcher.match("Queen - Bohemian Rhapsody", null, LIBRARY)
        assertEquals("Bohemian Rhapsody", matched?.title)
    }

    @Test
    fun matches_when_source_reverses_that_order() {
        val matched = TrackMatcher.match("Bohemian Rhapsody - Queen", null, LIBRARY)
        assertEquals("Bohemian Rhapsody", matched?.title)
    }

    @Test
    fun matches_cyrillic_titles() {
        val matched = TrackMatcher.match("Группа крови (Remastered)", "Кино", LIBRARY)
        assertEquals("Группа крови", matched?.title)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "ЛСП        | lsp",
            "Кино       | kino",
            "Ленинград  | leningrad",
            "Щербаков   | scherbakov",
            "Хаски      | khaski",
            "Ёлка       | elka",
            "Чайф       | chaif"
        ],
        delimiter = '|'
    )
    fun folds_cyrillic_onto_latin(raw: String, expected: String) {
        assertEquals(expected.trim(), TrackMatcher.normalize(raw.trim()))
    }

    /** Стриминг пишет «ЛСП» как «LSP»: между алфавитами нет ни одной общей триграммы. */
    @Test
    fun matches_artist_spelled_in_the_other_script() {
        val matched = TrackMatcher.match("Ты не он", "LSP", LIBRARY)
        assertEquals("Ты не он", matched?.title)
    }

    @Test
    fun matches_title_and_artist_both_transliterated() {
        val matched = TrackMatcher.match("Gruppa krovi", "Kino", LIBRARY)
        assertEquals("Группа крови", matched?.title)
    }

    /** Один артист на всю библиотеку не должен протаскивать чужое название через порог. */
    @Test
    fun does_not_match_a_different_song_by_the_same_artist() {
        val matched = TrackMatcher.match("Killer Queen", "Queen", LIBRARY)
        assertNull(matched)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Something Entirely Different", "Nothing Like It", "Zzzz"])
    fun returns_null_when_nothing_is_close(param: String) {
        assertNull(TrackMatcher.match(param, "Unknown Artist", LIBRARY))
    }

    @Test
    fun returns_null_for_blank_input() {
        assertNull(TrackMatcher.match("", "Queen", LIBRARY))
        assertNull(TrackMatcher.match(null, null, LIBRARY))
    }

    @Test
    fun returns_null_for_an_empty_library() {
        assertNull(TrackMatcher.match("Bohemian Rhapsody", "Queen", emptyList()))
    }

    private companion object {
        fun track(title: String, artist: String) =
            Track(UUID.randomUUID(), title, Artist(UUID.randomUUID(), artist))

        val LIBRARY = listOf(
            track("Bohemian Rhapsody", "Queen"),
            track("Under Pressure", "Queen"),
            track("Smells Like Teen Spirit", "Nirvana"),
            track("Группа крови", "Кино"),
            track("Ты не он", "ЛСП"),
            track("Café del Mar", "Energy 52")
        )
    }
}
