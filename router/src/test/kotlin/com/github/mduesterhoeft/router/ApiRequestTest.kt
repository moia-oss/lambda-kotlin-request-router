package com.github.mduesterhoeft.router

import assertk.assert
import assertk.assertions.isEqualTo
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Test

class ApiRequestTest {

    @Test
    fun `should match header`() {
        val request = APIGatewayProxyRequestEvent().withHeaders(mapOf("Accept" to "application/json"))

        assert(request.acceptHeader()).isEqualTo("application/json")
    }

    @Test
    fun `should match header lowercase`() {
        val request = APIGatewayProxyRequestEvent().withHeaders(mapOf("accept" to "application/json"))

        assert(request.acceptHeader()).isEqualTo("application/json")
    }

    @Test
    fun `should match header uppercase`() {
        val request = APIGatewayProxyRequestEvent().withHeaders(mapOf("ACCEPT" to "application/json"))

        assert(request.acceptHeader()).isEqualTo("application/json")
    }
}