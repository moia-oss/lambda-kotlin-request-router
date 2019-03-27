package com.github.mduesterhoeft.router.proto

import com.github.mduesterhoeft.router.JsonDeserializationHandler
import com.github.mduesterhoeft.router.JsonSerializationHandler
import com.github.mduesterhoeft.router.RequestHandler

abstract class ProtoEnabledRequestHandler : RequestHandler() {

    override fun serializationHandlers() =
        listOf(ProtoSerializationHandler(), JsonSerializationHandler(objectMapper))

    override fun deserializationHandlers() =
        listOf(ProtoDeserializationHandler(), JsonDeserializationHandler(objectMapper))
}