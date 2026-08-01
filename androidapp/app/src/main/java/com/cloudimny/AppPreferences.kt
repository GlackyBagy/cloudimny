package com.cloudimny

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFERENCES_NAME = "app_preferences"
private const val STATE_KEY = "is_authorized"

object AppPreferences {

    fun setAuthorized(context: Context, value: Boolean) {
        preferences(context, PREFERENCES_NAME).edit {
            putBoolean(STATE_KEY, value)
        }
    }

    fun getAuthorized(context: Context): Boolean =
        preferences(context, PREFERENCES_NAME).getBoolean(STATE_KEY, false)

    fun preferences(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
}