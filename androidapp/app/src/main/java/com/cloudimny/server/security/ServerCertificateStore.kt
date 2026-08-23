package com.cloudimny.server.security

import android.annotation.SuppressLint
import android.content.Context
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.core.content.edit
import com.cloudimny.AppPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val SERVER_PREFERENCES_NAME = "server_data"
private const val FINGERPRINT_KEY = "certificate_sha256_fingerprint"
private const val HOST_KEY = "server_host"
private const val AUTH_SECRET_KEY = "auth_secret"
private const val SHA256_HEX_LENGTH = 64

object ServerCertificateStore {
    fun save(context: Context, fingerprint: String, host: String, authSecret: String) {
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME).edit {
            putString(FINGERPRINT_KEY, normalize(fingerprint))
            putString(HOST_KEY, normalizeHost(host))
            putString(AUTH_SECRET_KEY, authSecret)
        }
    }

    fun saveHost(context: Context, host: String) {
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME).edit {
            putString(HOST_KEY, normalizeHost(host))
        }
    }

    fun isValidHost(host: String): Boolean =
        host.isNotBlank() && "https://${normalizeHost(host)}/".toHttpUrlOrNull() != null

    /** Accepts the digest in either form the tooling produces — colon-separated or bare hex. */
    fun isValidFingerprint(fingerprint: String): Boolean {
        val normalized = normalize(fingerprint)
        return normalized.length == SHA256_HEX_LENGTH &&
                normalized.all { it in '0'..'9' || it in 'A'..'F' }
    }

    fun normalizeHost(host: String): String {
        val trimmed = host.trim()
        if (trimmed.startsWith("[")) return trimmed
        return if (trimmed.count { it == ':' } > 1) "[$trimmed]" else trimmed
    }

    fun fingerprint(context: Context): String? =
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME)
            .getString(FINGERPRINT_KEY, null)

    fun host(context: Context): String? =
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME).getString(HOST_KEY, null)

    fun authSecret(context: Context): String? =
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME).getString(AUTH_SECRET_KEY, null)

    fun sslContext(context: Context): SSLContext {
        val trustManager = trustManager(context)
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), null)
        }
    }

    @SuppressLint("CustomX509TrustManager")
    fun trustManager(context: Context): X509TrustManager {
        val pinnedFingerprint = fingerprint(context)
            ?: throw IllegalStateException("Server certificate fingerprint is not saved")

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
                Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
                val leaf = chain.firstOrNull()
                    ?: throw CertificateException("Certificate chain is empty")

                if (normalize(fingerprintOf(leaf)) != pinnedFingerprint) {
                    throw CertificateException("Server certificate does not match the pinned fingerprint")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }

    private fun fingerprintOf(certificate: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun normalize(fingerprint: String): String =
        fingerprint.replace(":", "").replace(" ", "").trim().uppercase()

}
