package io.moia.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.MediaType
import isCompatibleWith

interface SerializationHandler {

    fun supports(acceptHeader: MediaType, body: Any): Boolean

    fun serialize(acceptHeader: MediaType, body: Any): String
}

class SerializationHandlerChain(private val handlers: List<SerializationHandler>) :
    SerializationHandler {

    override fun supports(acceptHeader: MediaType, body: Any): Boolean =
        handlers.any { it.supports(acceptHeader, body) }

    override fun serialize(acceptHeader: MediaType, body: Any): String =
        handlers.first { it.supports(acceptHeader, body) }.serialize(acceptHeader, body)
}

class JsonSerializationHandler(private val objectMapper: ObjectMapper) : SerializationHandler {

    private val json = MediaType.parse("application/json")
    private val jsonStructuredSuffixWildcard = MediaType.parse("application/*+json")

    override fun supports(acceptHeader: MediaType, body: Any): Boolean =
        json.isCompatibleWith(acceptHeader) || jsonStructuredSuffixWildcard.isCompatibleWith(acceptHeader)

    override fun serialize(acceptHeader: MediaType, body: Any): String =
        objectMapper.writeValueAsString(body)
}