package io.moia.router

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.ContentType

interface SerializationHandler {

    fun supports(acceptHeader: ContentType, response: ResponseEntity<*>): Boolean

    fun serialize(acceptHeader: ContentType, response: ResponseEntity<*>): String
}

class SerializationHandlerChain(private val handlers: List<SerializationHandler>) :
    SerializationHandler {

    override fun supports(acceptHeader: ContentType, response: ResponseEntity<*>): Boolean =
        handlers.any { it.supports(acceptHeader, response) }

    override fun serialize(acceptHeader: ContentType, response: ResponseEntity<*>): String =
        handlers.first { it.supports(acceptHeader, response) }.serialize(acceptHeader, response)
}

class JsonSerializationHandler(private val objectMapper: ObjectMapper) : SerializationHandler {

    private val json = ContentType.parse("application/json")

    override fun supports(acceptHeader: ContentType, response: ResponseEntity<*>): Boolean = acceptHeader.mimeType == json.mimeType

    override fun serialize(acceptHeader: ContentType, response: ResponseEntity<*>): String =
        objectMapper.writeValueAsString(response.body)
}