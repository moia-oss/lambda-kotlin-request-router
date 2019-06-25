package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonDeserializationHandlerTest {

    val deserializationHandler = JsonDeserializationHandler(jacksonObjectMapper())

    @Test
    fun `should support json`() {
        assertTrue(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "application/json")))
        assertTrue(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "application/vnd.moia.v1+json")))
    }

    @Test
    fun `should not support anything else than json`() {
        assertFalse(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "image/png")))
        assertFalse(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "text/plain")))
    }
}