package com.cloudimny.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cloudimny.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "ServerErrorHandling"

/** Reports through the fragment, which stays silent once it is detached. */
suspend fun Fragment.runCatchingServerErrors(block: suspend CoroutineScope.() -> Unit) {
    val messageResId = catchServerErrors(this::class.simpleName, block) ?: return
    if (!isAdded) return
    Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
}

/** The same, for callers with no fragment at hand — a helper driven by a view, say. */
suspend fun Context.runCatchingServerErrors(block: suspend CoroutineScope.() -> Unit) {
    val messageResId = catchServerErrors(this::class.simpleName, block) ?: return
    Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
}

/**
 * Runs [block] without letting a failed request take the app down, and returns the message to
 * report — or null when [block] went through.
 *
 * Only the failures that really mean "the server did not answer" are reported as such — anything
 * else is a bug in our own code, and dressing it up as a connection problem is how a crash turns
 * into a mystery. Those still do not crash the screen, but they say so plainly and land in the log
 * with a stack trace. [CancellationException] is rethrown so leaving the screen still cancels the
 * request in flight.
 *
 * [block] runs inside its own [coroutineScope], and receives it: a coroutine an implementation
 * starts with `async` becomes a child of that scope rather than of the caller's. That is what
 * makes the catch below reach a parallel request at all — a failing `async` cancels its parent
 * job the moment it throws, without waiting to be awaited, so a `try` wrapped around `await()`
 * alone catches the exception only after the enclosing `launch` has already crashed the app.
 */
private suspend fun catchServerErrors(
    owner: String?,
    block: suspend CoroutineScope.() -> Unit
): Int? =
    try {
        coroutineScope { block() }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        R.string.cannot_connect_message
    } catch (e: HttpException) {
        R.string.cannot_connect_message
    } catch (e: IllegalStateException) {
        // сервер ещё не настроен: базовый URL не из чего построить
        R.string.cannot_connect_message
    } catch (e: Exception) {
        Log.e(TAG, "unexpected failure in $owner", e)
        R.string.unknown_error_message
    }
