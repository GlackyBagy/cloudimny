package com.cloudimny.models.matching

import com.cloudimny.models.meta.Track
import java.text.Normalizer

/**
 * Decorations that carry no identity: what a streaming service appends to a title and a local tag
 * usually does not, or the other way round. Matched as whole words, so "live" does not eat "Living".
 */
private val NOISE_WORDS = setOf(
    "feat", "ft", "featuring", "with",
    "remaster", "remastered", "remix", "mix",
    "official", "video", "audio", "lyric", "lyrics", "visualizer",
    "live", "acoustic", "instrumental",
    "radio", "edit", "single", "album", "version", "original",
    "explicit", "clean", "bonus", "deluxe", "extended",
    "mono", "stereo", "hd", "hq", "mv", "prod"
)

private val BRACKETED = Regex("[(\\[{][^)\\]}]*[)\\]}]")
private val FEATURING_TAIL = Regex("\\b(feat|ft|featuring)\\b.*$")
private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
private val COMBINING_MARKS = Regex("\\p{Mn}+")

private const val TRIGRAM_SIZE = 3

/**
 * Streaming services spell many Cyrillic acts in Latin — "ЛСП" is listed as "LSP" — and across
 * scripts the two share no trigram at all, so the artist scores a flat zero and takes an otherwise
 * perfect title down with it. Folding one script onto the other is what makes them comparable.
 *
 * The exact scheme matters less than using one consistently: both sides pass through here, and
 * trigrams absorb the places where a service transliterated differently than this table does.
 *
 * Applied after NFD, so "й" and "ё" have already lost their marks and arrive as "и" and "е".
 */
private val CYRILLIC_TO_LATIN = mapOf(
    'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "e",
    'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
    'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
    'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sch",
    'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
    // соседние кириллицы попадают в те же библиотеки
    'і' to "i", 'ї' to "yi", 'є' to "ye", 'ґ' to "g", 'ў' to "u"
)

/**
 * Finds the library track a foreign player is currently playing.
 *
 * Two stages, because they fail in different ways. Normalisation strips the decorations that make
 * two spellings of the same recording differ — brackets, "feat." tails, punctuation, diacritics —
 * and catches most of the library outright. What survives that is scored by trigram overlap, which,
 * unlike edit distance, is indifferent to word order and degrades gracefully on a single typo.
 *
 * Title and artist are scored apart and weighted rather than concatenated: a library full of one
 * artist would otherwise let the artist half carry a wrong title over the threshold.
 */
object TrackMatcher {
    const val DEFAULT_THRESHOLD = 0.62

    private const val TITLE_WEIGHT = 0.6
    private const val ARTIST_WEIGHT = 0.4

    /** No weighting rescues a field this far off; below it the pair is not the same recording. */
    private const val MIN_FIELD_SCORE = 0.35

    fun match(
        sourceTitle: String?,
        sourceArtist: String?,
        library: List<Track>,
        threshold: Double = DEFAULT_THRESHOLD
    ): Track? {
        val title = normalize(sourceTitle.orEmpty())
        val artist = normalize(sourceArtist.orEmpty())
        if (title.isEmpty()) return null

        best(title, artist, library, threshold)?.let { return it }

        // YouTube и подобные кладут "Артист - Название" целиком в title, оставляя artist пустым;
        // какая половина чем является — заранее не известно, поэтому пробуем обе
        if (artist.isEmpty()) {
            val parts = splitCombined(sourceTitle.orEmpty())
            for ((candidateTitle, candidateArtist) in parts) {
                best(normalize(candidateTitle), normalize(candidateArtist), library, threshold)
                    ?.let { return it }
            }
        }

        return null
    }

    private fun best(
        title: String,
        artist: String,
        library: List<Track>,
        threshold: Double
    ): Track? {
        if (title.isEmpty()) return null

        var best: Track? = null
        var bestScore = 0.0

        for (track in library) {
            val trackTitle = normalize(track.title.orEmpty())
            if (trackTitle.isEmpty()) continue
            val trackArtist = normalize(track.artist?.nickname.orEmpty())

            // после нормализации совпало буквально — считать нечего
            if (trackTitle == title && (artist.isEmpty() || trackArtist == artist)) return track

            val titleScore = similarity(title, trackTitle)
            if (titleScore < MIN_FIELD_SCORE) continue

            // артиста может не быть ни у источника, ни у нас: тогда судим по названию
            val artistScore =
                if (artist.isEmpty() || trackArtist.isEmpty()) titleScore
                else similarity(artist, trackArtist)
            if (artistScore < MIN_FIELD_SCORE) continue

            val score = TITLE_WEIGHT * titleScore + ARTIST_WEIGHT * artistScore
            if (score > bestScore) {
                bestScore = score
                best = track
            }
        }

        return if (bestScore >= threshold) best else null
    }

    /** Both readings of an "A - B" string, as title to artist. */
    private fun splitCombined(value: String): List<Pair<String, String>> {
        val separator = value.indexOf(" - ")
        if (separator <= 0) return emptyList()

        val left = value.substring(0, separator)
        val right = value.substring(separator + 3)
        return listOf(right to left, left to right)
    }

    /**
     * Reduces a title or an artist to the part that identifies it: noise brackets, then a trailing
     * "feat. …", then everything that is not a letter or a digit — which also flattens the
     * difference between "Simon & Garfunkel" and "Simon and Garfunkel" into the same two words.
     * Cyrillic is folded to Latin along the way, so a name spelled in either script compares equal.
     */
    fun normalize(value: String): String {
        val withoutMarks = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .let(::transliterate)

        val withoutBrackets = stripNoiseBrackets(withoutMarks)
        // скобки съели всё — значит шумом они не были, откатываемся
        val base = if (withoutBrackets.isBlank()) withoutMarks else withoutBrackets

        val words = base
            .replace(FEATURING_TAIL, " ")
            .replace(NON_ALPHANUMERIC, " ")
            .trim()
            .split(" ")
            .filter { it.isNotEmpty() && it !in NOISE_WORDS }

        // всё оказалось шумом — значит шумом оно не было, откатываемся к словам как есть
        if (words.isEmpty()) {
            return base.replace(NON_ALPHANUMERIC, " ").trim()
        }

        return words.joinToString(" ")
    }

    /**
     * Drops a bracketed group only when something inside it marks it as decoration, so
     * "(Remastered 2011)" and "[Official Video]" go while "(Don't Fear) The Reaper" keeps the half
     * of its name that happens to be parenthesised.
     */
    private fun transliterate(value: String): String {
        if (value.none { it in CYRILLIC_TO_LATIN }) return value

        return buildString(value.length) {
            for (char in value) append(CYRILLIC_TO_LATIN[char] ?: char)
        }
    }

    private fun stripNoiseBrackets(value: String): String =
        BRACKETED.replace(value) { match ->
            val inner = match.value.drop(1).dropLast(1)
            val words = inner.split(NON_ALPHANUMERIC).filter { it.isNotEmpty() }
            if (words.any { it in NOISE_WORDS }) " " else " $inner "
        }

    /**
     * Dice coefficient over trigrams, the measure `pg_trgm` uses. Padding the ends makes the first
     * and last characters count as much as the middle ones.
     */
    fun similarity(first: String, second: String): Double {
        if (first == second) return 1.0
        if (first.isEmpty() || second.isEmpty()) return 0.0

        val a = trigrams(first)
        val b = trigrams(second)
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val shared = a.count { it in b }
        return 2.0 * shared / (a.size + b.size)
    }

    private fun trigrams(value: String): Set<String> {
        val padded = "  $value "
        if (padded.length < TRIGRAM_SIZE) return emptySet()

        return buildSet {
            for (index in 0..padded.length - TRIGRAM_SIZE) {
                add(padded.substring(index, index + TRIGRAM_SIZE))
            }
        }
    }
}
