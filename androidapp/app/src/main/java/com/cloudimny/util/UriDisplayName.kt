package com.cloudimny.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameColumn)
            }
        }
    return uri.lastPathSegment ?: "track"
}
