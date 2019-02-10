package com.github.mduesterhoeft.router

import assertk.assert
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ApiRequestTest {

    @Test
    fun `should match header`() {
        val request = ApiRequest(headers = mutableMapOf("Accept" to "application/json"))

        assert(request.acceptHeader).isEqualTo("application/json")
    }

    @Test
    fun `should match header lowercase`() {
        val request = ApiRequest(headers = mutableMapOf("accept" to "application/json"))

        assert(request.acceptHeader).isEqualTo("application/json")
    }

    @Test
    fun `should match header uppercase`() {
        val request = ApiRequest(headers = mutableMapOf("ACCEPT" to "application/json"))

        assert(request.acceptHeader).isEqualTo("application/json")
    }
}