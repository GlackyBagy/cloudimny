package com.cloudimny.server

import com.cloudimny.server.security.ServerCertificateStore
import org.json.JSONException
import org.json.JSONObject

private const val HOST_KEY = "host"
private const val FINGERPRINT_KEY = "fingerprint"
private const val AUTH_SECRET_KEY = "authSecret"
private const val BYTE_ORDER_MARK = "\uFEFF"

/**
 * Everything another client needs to reach the same server. Written by the export in settings and
 * read back by the import during setup — both sides live here so the format cannot drift.
 */
data class ServerExport(val host: String, val fingerprint: String, val authSecret: String)

fun buildServerExport(export: ServerExport): String =
    JSONObject()
        .put(HOST_KEY, export.host)
        .put(FINGERPRINT_KEY, export.fingerprint)
        .put(AUTH_SECRET_KEY, export.authSecret)
        .toString(2)

/**
 * Parses an exported payload, returning null for anything that is not one: the file is picked by
 * the user, so an unrelated document is an ordinary outcome rather than an error to report in
 * detail. Values are checked, not just present — a fingerprint that cannot be a SHA-256 digest
 * would otherwise be stored and only fail later, at the first request, as a pinning mismatch.
 */
fun parseServerExport(payload: String): ServerExport? {
    val json = try {
        // BOM переживает поездку файла через десктоп, а JSONObject на нём спотыкается
        JSONObject(payload.removePrefix(BYTE_ORDER_MARK))
    } catch (_: JSONException) {
        return null
    }

    val host = json.optString(HOST_KEY).trim()
    val fingerprint = json.optString(FINGERPRINT_KEY).trim()
    val authSecret = json.optString(AUTH_SECRET_KEY).trim()

    if (!ServerCertificateStore.isValidHost(host)) return null
    if (!ServerCertificateStore.isValidFingerprint(fingerprint)) return null
    if (authSecret.isEmpty()) return null

    return ServerExport(host, fingerprint, authSecret)
}
