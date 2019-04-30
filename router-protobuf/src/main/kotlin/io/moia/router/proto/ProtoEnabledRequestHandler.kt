package io.moia.router.proto

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.common.net.MediaType
import io.moia.router.JsonDeserializationHandler
import io.moia.router.JsonSerializationHandler
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity

abstract class ProtoEnabledRequestHandler : RequestHandler() {

    override fun serializationHandlers() =
        listOf(ProtoSerializationHandler(), JsonSerializationHandler(objectMapper))

    override fun deserializationHandlers() =
        listOf(ProtoDeserializationHandler(), JsonDeserializationHandler(objectMapper))

    override fun <T> createResponse(
        contentType: MediaType?,
        input: APIGatewayProxyRequestEvent,
        response: ResponseEntity<T>
    ): APIGatewayProxyResponseEvent {
        return super.createResponse(contentType, input, response).withIsBase64Encoded(true)
    }
}