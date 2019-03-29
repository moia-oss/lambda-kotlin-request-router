package io.moia.router.proto

import io.moia.router.JsonDeserializationHandler
import io.moia.router.JsonSerializationHandler
import io.moia.router.RequestHandler

abstract class ProtoEnabledRequestHandler : RequestHandler() {

    override fun serializationHandlers() =
        listOf(ProtoSerializationHandler(), JsonSerializationHandler(objectMapper))

    override fun deserializationHandlers() =
        listOf(ProtoDeserializationHandler(), JsonDeserializationHandler(objectMapper))
}