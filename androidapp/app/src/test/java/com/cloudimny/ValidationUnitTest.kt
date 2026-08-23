package com.cloudimny

import com.cloudimny.models.SshAuthMethod
import com.cloudimny.models.SshConnectionCredentials
import com.cloudimny.models.validation.SshValidationResult.*
import com.cloudimny.models.validation.validateSshCredentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ValidationUnitTest {
    @ParameterizedTest
    @ValueSource(strings = ["11.123.3.3", "domain.com", "1.1.1.1"])
    fun invalid_when_port_not_stated(param: String) {
        val credentials = SshConnectionCredentials(param, "user", "password")
        assertEquals(INVALID_ADDRESS, validateSshCredentials(credentials))
    }

    @ParameterizedTest
    @ValueSource(strings = ["256.23.33.1", "13.1.1"])
    fun invalid_when_wrong_ip_stated(param: String) {
        val credentials = SshConnectionCredentials(param, "user", "password")
        assertEquals(INVALID_ADDRESS, validateSshCredentials(credentials))
    }

    @ParameterizedTest
    @ValueSource(strings = ["2001:db8::1:22", "2001:db8::1", "[2001:db8::1]", "[2001:db8::1]22", "[]:22"])
    fun invalid_when_ipv6_malformed(param: String) {
        val credentials = SshConnectionCredentials(param, "user", "password")
        assertEquals(INVALID_ADDRESS, validateSshCredentials(credentials))
    }

    @ParameterizedTest
    @ValueSource(strings = ["123.123.123.123:123", "1.1.1.1:22", "[2001:db8::1]:22", "[::1]:22"])
    fun valid_when_address_correct(param: String) {
        val credentials = SshConnectionCredentials(param, "user", "password")
        assertEquals(VALID, validateSshCredentials(credentials))
    }

    @Test
    fun valid_when_openssh_key_pasted_whole() {
        assertEquals(VALID, validateKey(OPENSSH_KEY))
    }

    @Test
    fun valid_when_encrypted_pem_key_carries_headers() {
        assertEquals(VALID, validateKey(ENCRYPTED_RSA_KEY))
    }

    @Test
    fun valid_when_key_pasted_with_crlf_and_trailing_blank_lines() {
        assertEquals(VALID, validateKey(OPENSSH_KEY.replace("\n", "\r\n") + "\r\n\r\n"))
    }

    /** Пароль в режиме ключа уходит только в sudo, поэтому пустой — это норма. */
    @Test
    fun valid_when_key_used_and_password_empty() {
        val credentials = SshConnectionCredentials(
            address = "1.1.1.1:22",
            username = "user",
            password = "",
            privateKey = OPENSSH_KEY,
            authMethod = SshAuthMethod.KEY
        )
        assertEquals(VALID, validateSshCredentials(credentials))
    }

    @Test
    fun invalid_when_begin_and_end_disagree_on_key_type() {
        val mismatched = OPENSSH_KEY.replace("-----END OPENSSH", "-----END RSA")
        assertEquals(INVALID_KEY, validateKey(mismatched))
    }

    @Test
    fun invalid_when_armour_missing() {
        val body = OPENSSH_KEY.lines().drop(1).dropLast(1).joinToString("\n")
        assertEquals(INVALID_KEY, validateKey(body))
    }

    @Test
    fun invalid_when_end_line_missing() {
        assertEquals(INVALID_KEY, validateKey(OPENSSH_KEY.substringBefore("-----END")))
    }

    @Test
    fun invalid_when_body_is_not_base64() {
        val broken = OPENSSH_KEY.replace("b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQ", "not base64!")
        assertEquals(INVALID_KEY, validateKey(broken))
    }

    @Test
    fun invalid_when_body_is_empty() {
        val empty = "-----BEGIN OPENSSH PRIVATE KEY-----\n-----END OPENSSH PRIVATE KEY-----"
        assertEquals(INVALID_KEY, validateKey(empty))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/home/user/.ssh/id_ed25519", "~/.ssh/id_rsa", "id_rsa", ""])
    fun invalid_when_path_pasted_instead_of_content(param: String) {
        assertEquals(INVALID_KEY, validateKey(param))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGhdUt2UdVfUVJAGIkM9SI8n user@host",
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDIQVLdlHVX1FSQBiJDPUiP user@host",
            "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTY= user@host"
        ]
    )
    fun invalid_when_public_key_pasted(param: String) {
        assertEquals(INVALID_KEY_IS_PUBLIC, validateKey(param))
    }

    private fun validateKey(privateKey: String) = validateSshCredentials(
        SshConnectionCredentials(
            address = "1.1.1.1:22",
            username = "user",
            password = "password",
            privateKey = privateKey,
            authMethod = SshAuthMethod.KEY
        )
    )

    private companion object {
        val OPENSSH_KEY = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gt
            ZWQyNTUxOQAAACBoXVLdlHVX1FSQBiJDPUiPJ7RfPFYPUmSpsHVoOZmz3wAAAJhVQ2Ok
            VUNjpAAAAAtzc2gtZWQyNTUxOQAAACBoXVLdlHVX1FSQBiJDPUiPJ7RfPFYPUmSpsHVo
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()

        val ENCRYPTED_RSA_KEY = """
            -----BEGIN RSA PRIVATE KEY-----
            Proc-Type: 4,ENCRYPTED
            DEK-Info: AES-128-CBC,9A1B2C3D4E5F60718293A4B5C6D7E8F9

            hQEMA0v0kR0AAAABCAAgTm90YXJlYWxrZXlqdXN0YmFzZTY0cGFkZGluZ2hlcmU
            PWFuZG1vcmViYXNlNjRjb250ZW50Zm9ydGhldGVzdHN1aXRldG9jaGV3b25QUQ==
            -----END RSA PRIVATE KEY-----
        """.trimIndent()
    }
}
