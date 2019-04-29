package io.moia.router.proto

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.moia.router.withHeader
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ProtoDeserializationHandlerTest {

    @Test
    fun `Deserializer should not support if the content type of the input is null`() {
        assertFalse(ProtoDeserializationHandler().supports(APIGatewayProxyRequestEvent()))
    }

    @Test
    fun `Deserializer should support if the content type of the input is protobuf`() {
        assertTrue(ProtoDeserializationHandler().supports(APIGatewayProxyRequestEvent().withHeader("content-type", "application/x-protobuf")))
    }
}