package io.moia.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.MediaType

interface SerializationHandler {

    fun supports(acceptHeader: MediaType, response: ResponseEntity<*>): Boolean

    fun serialize(acceptHeader: MediaType, response: ResponseEntity<*>): String
}

class SerializationHandlerChain(private val handlers: List<SerializationHandler>) :
    SerializationHandler {

    override fun supports(acceptHeader: MediaType, response: ResponseEntity<*>): Boolean =
        handlers.any { it.supports(acceptHeader, response) }

    override fun serialize(acceptHeader: MediaType, response: ResponseEntity<*>): String =
        handlers.first { it.supports(acceptHeader, response) }.serialize(acceptHeader, response)
}

class JsonSerializationHandler(private val objectMapper: ObjectMapper) : SerializationHandler {

    private val json = MediaType.parse("application/json")

    override fun supports(acceptHeader: MediaType, response: ResponseEntity<*>): Boolean = acceptHeader.`is`(json)

    override fun serialize(acceptHeader: MediaType, response: ResponseEntity<*>): String =
        objectMapper.writeValueAsString(response.body)
}