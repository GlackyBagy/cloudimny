package com.cloudimny.models.validation

import com.cloudimny.models.SshAuthMethod
import com.cloudimny.models.SshConnectionCredentials

private val PRIVATE_KEY_BEGIN = Regex("^-----BEGIN ([A-Z0-9 ]*)PRIVATE KEY-----$")

/** RFC 1421 headers an encrypted PEM key carries between the armour and the body. */
private val PEM_HEADER = Regex("^[A-Za-z][A-Za-z0-9-]*: .*$")
private val BASE64_LINE = Regex("^[A-Za-z0-9+/]+={0,2}$")
private val PUBLIC_KEY_PREFIXES =
    listOf("ssh-rsa", "ssh-ed25519", "ssh-dss", "ecdsa-sha2-", "sk-ssh-", "sk-ecdsa-")

/** BEGIN, at least one line of body, END. */
private const val MIN_PRIVATE_KEY_LINES = 3


fun validateSshCredentials(credentials: SshConnectionCredentials): SshValidationResult {
    if (!validateAddress(credentials.address))
        return SshValidationResult.INVALID_ADDRESS
    if (!validateUsername(credentials.username))
        return SshValidationResult.INVALID_USERNAME

    if (credentials.authMethod == SshAuthMethod.KEY)
        return validatePrivateKey(credentials.privateKey)

    if (!validatePassword(credentials.password))
        return SshValidationResult.INVALID_PASSWORD
    return SshValidationResult.VALID
}

private fun validateAddress(address: String): Boolean {
    val separatorIndex = address.lastIndexOf(':')
    if (separatorIndex <= 0 || separatorIndex == address.length - 1)
        return false

    val host = address.substring(0, separatorIndex)
    val port = address.substring(separatorIndex + 1).toIntOrNull() ?: return false

    return host.isNotEmpty() && port in 1..65535
}

private fun validateUsername(username: String): Boolean {
    return username.isNotEmpty()
}

private fun validatePassword(password: String): Boolean {
    return password.isNotEmpty()
}

/**
 * Accepts nothing but a private key pasted whole: the armour has to be intact and self-consistent —
 * BEGIN and END naming the same key type — and everything between them has to be base64, optionally
 * behind the headers an encrypted PEM key carries.
 *
 * The point of being this strict is that the alternatives users reach for all fail silently
 * otherwise. A path to the key file, the public half of the pair, a fragment copied without its
 * first and last lines — each of those would reach the SSH library and come back seconds later as
 * an opaque authentication failure, with nothing pointing at the paste as the cause.
 */
private fun validatePrivateKey(privateKey: String): SshValidationResult {
    val trimmed = privateKey.trim()
    if (looksLikePublicKey(trimmed)) return SshValidationResult.INVALID_KEY_IS_PUBLIC

    // пустые строки выкидываем целиком: они разделяют заголовки и тело у зашифрованных
    // ключей и остаются хвостом после вставки, но структуру не несут
    val lines = trimmed.lines().map(String::trim).filter(String::isNotEmpty)
    if (lines.size < MIN_PRIVATE_KEY_LINES) return SshValidationResult.INVALID_KEY

    val keyType = PRIVATE_KEY_BEGIN.matchEntire(lines.first())?.groupValues?.get(1)
        ?: return SshValidationResult.INVALID_KEY
    if (lines.last() != "-----END ${keyType}PRIVATE KEY-----")
        return SshValidationResult.INVALID_KEY

    val body = lines.subList(1, lines.size - 1).dropWhile { PEM_HEADER.matches(it) }
    if (body.isEmpty() || !body.all { BASE64_LINE.matches(it) })
        return SshValidationResult.INVALID_KEY

    return SshValidationResult.VALID
}

private fun looksLikePublicKey(privateKey: String): Boolean =
    privateKey.startsWith("-----BEGIN PUBLIC KEY-----") ||
            PUBLIC_KEY_PREFIXES.any { privateKey.startsWith(it) }


enum class SshValidationResult {
    VALID,
    INVALID_ADDRESS,
    INVALID_USERNAME,
    INVALID_PASSWORD,
    INVALID_KEY,
    INVALID_KEY_IS_PUBLIC
}
