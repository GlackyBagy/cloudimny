package com.cloudimny.models.validation

import com.cloudimny.models.SshConnectionCredentials


fun validateSshCredentials(credentials: SshConnectionCredentials): SshValidationResult {
    if (!validateAddress(credentials.address))
        return SshValidationResult.INVALID_ADDRESS
    if (!validateUsername(credentials.username))
        return SshValidationResult.INVALID_USERNAME
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


enum class SshValidationResult {
    VALID,
    INVALID_ADDRESS,
    INVALID_USERNAME,
    INVALID_PASSWORD
}