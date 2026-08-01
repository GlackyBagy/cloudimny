package com.cloudimny

import com.cloudimny.models.SshConnectionCredentials
import com.cloudimny.models.validation.SshValidationResult.*
import com.cloudimny.models.validation.validateSshCredentials
import org.junit.jupiter.api.Assertions.assertEquals
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
    @ValueSource(strings = ["123.123.123.123:123", "1.1.1.1:22"])
    fun valid_when_address_correct(param: String) {
        val credentials = SshConnectionCredentials(param, "user", "password")
        assertEquals(VALID, validateSshCredentials(credentials))
    }
}