package com.cloudimny.models

data class SshConnectionCredentials(
    val address: String,
    val username: String,
    val password: String
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
