package io.moia.router.proto

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.common.net.MediaType
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity

abstract class ProtoEnabledRequestHandler : RequestHandler() {

    override fun serializationHandlers() =
        listOf(ProtoSerializationHandler()) + super.serializationHandlers()

    override fun deserializationHandlers() =
        listOf(ProtoDeserializationHandler()) + super.deserializationHandlers()

    override fun <T> createResponse(
        contentType: MediaType,
        response: ResponseEntity<T>
    ): APIGatewayProxyResponseEvent {
        return super.createResponse(contentType, response).withIsBase64Encoded(true)
    }
}
