package com.cloudimny.util

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cloudimny.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

/**
 * Runs [block], turning any failure to talk to the server (no connection, host not configured,
 * non-2xx response, malformed response body, ...) into a toast instead of an uncaught exception
 * that would crash the app. [CancellationException] is rethrown so navigating away from the
 * screen still cancels the in-flight request as normal.
 *
 * [block] runs inside its own [coroutineScope], and receives it: a coroutine an implementation
 * starts with `async` becomes a child of that scope rather than of the caller's. That is what
 * makes the catch below reach a parallel request at all — a failing `async` cancels its parent
 * job the moment it throws, without waiting to be awaited, so a `try` wrapped around `await()`
 * alone catches the exception only after the enclosing `launch` has already crashed the app.
 */
suspend fun Fragment.runCatchingServerErrors(block: suspend CoroutineScope.() -> Unit) {
    try {
        coroutineScope { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        showServerUnreachable()
    }
}

private fun Fragment.showServerUnreachable() {
    if (!isAdded) return
    Toast.makeText(requireContext(), R.string.cannot_connect_message, Toast.LENGTH_SHORT).show()
}
