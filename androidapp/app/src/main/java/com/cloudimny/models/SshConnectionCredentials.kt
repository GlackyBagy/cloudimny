package com.cloudimny.models

enum class SshAuthMethod { PASSWORD, KEY }

data class SshConnectionCredentials(
    val address: String,
    val username: String,
    /**
     * With [SshAuthMethod.KEY] this is not the SSH password but the one `sudo -S` is fed on the
     * server, and may be empty when sudo there is passwordless.
     */
    val password: String,
    val privateKey: String = "",
    val authMethod: SshAuthMethod = SshAuthMethod.PASSWORD
) {
    private val splitAddress: List<String> by lazy { address.split(":") }
    val port: Int by lazy { Integer.parseInt(splitAddress[splitAddress.lastIndex]) }
    val host: String by lazy {
        var res: String =
            splitAddress.subList(0, splitAddress.lastIndex).joinToString(separator = ":")
        if (res.matches("^\\[.+]$".toRegex())) // removing [] from IPv6 in URL notation
            res.substring(1, res.length - 1)
        else
            res
    }
}
