package com.cloudimny

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFERENCES_NAME = "app_preferences"
private const val STATE_KEY = "is_authorized"
private const val TRACK_CACHE_KEY = "track_cache_mb"
private const val COVER_CACHE_KEY = "cover_cache_mb"

object AppPreferences {

    /** Cache bounds, in megabytes. Each step divides its range evenly, as Slider requires. */
    const val MIN_TRACK_CACHE_MB = 128
    const val MAX_TRACK_CACHE_MB = 4096
    const val TRACK_CACHE_STEP_MB = 128
    const val DEFAULT_TRACK_CACHE_MB = 512

    const val MIN_COVER_CACHE_MB = 16
    const val MAX_COVER_CACHE_MB = 512
    const val COVER_CACHE_STEP_MB = 16
    const val DEFAULT_COVER_CACHE_MB = 32

    fun setAuthorized(context: Context, value: Boolean) {
        preferences(context, PREFERENCES_NAME).edit {
            putBoolean(STATE_KEY, value)
        }
    }

    fun getAuthorized(context: Context): Boolean =
        preferences(context, PREFERENCES_NAME).getBoolean(STATE_KEY, false)

    /**
     * How much room the downloaded audio may take on disk. Kept here, among the settings that
     * describe this device, rather than with the server data: the export carries what another
     * client needs to reach the same server, and a cache budget chosen for this phone is not that.
     */
    fun setTrackCacheMb(context: Context, megabytes: Int) {
        preferences(context, PREFERENCES_NAME).edit {
            putInt(TRACK_CACHE_KEY, megabytes)
        }
    }

    fun getTrackCacheMb(context: Context): Int =
        alignToStep(
            preferences(context, PREFERENCES_NAME).getInt(TRACK_CACHE_KEY, DEFAULT_TRACK_CACHE_MB),
            MIN_TRACK_CACHE_MB, MAX_TRACK_CACHE_MB, TRACK_CACHE_STEP_MB
        )

    /** Counterpart of [setTrackCacheMb] for artwork; see it for why this lives outside the export. */
    fun setCoverCacheMb(context: Context, megabytes: Int) {
        preferences(context, PREFERENCES_NAME).edit {
            putInt(COVER_CACHE_KEY, megabytes)
        }
    }

    fun getCoverCacheMb(context: Context): Int =
        alignToStep(
            preferences(context, PREFERENCES_NAME).getInt(COVER_CACHE_KEY, DEFAULT_COVER_CACHE_MB),
            MIN_COVER_CACHE_MB, MAX_COVER_CACHE_MB, COVER_CACHE_STEP_MB
        )

    /**
     * Snaps a stored value onto the slider's grid. Nothing writes an unaligned one today, but the
     * slider throws on a value that is not a multiple of its step, and that would take the whole
     * settings screen down — cheap insurance against the bounds ever being retuned.
     */
    private fun alignToStep(megabytes: Int, min: Int, max: Int, step: Int): Int {
        val steps = (megabytes - min + step / 2) / step
        return (min + steps * step).coerceIn(min, max)
    }

    fun preferences(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
}
