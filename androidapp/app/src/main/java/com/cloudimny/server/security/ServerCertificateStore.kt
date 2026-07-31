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

private const val SERVER_PREFERENCES_NAME = "server_data"
private const val FINGERPRINT_KEY = "certificate_sha256_fingerprint"
private const val HOST_KEY = "server_host"

object ServerCertificateStore {
    fun save(context: Context, fingerprint: String, host: String) {
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME).edit {
            putString(FINGERPRINT_KEY, normalize(fingerprint))
            putString(HOST_KEY, host)
        }
    }

    fun fingerprint(context: Context): String? =
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME)
            .getString(FINGERPRINT_KEY, null)

    fun host(context: Context): String? =
        AppPreferences.preferences(context, SERVER_PREFERENCES_NAME).getString(HOST_KEY, null)

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
