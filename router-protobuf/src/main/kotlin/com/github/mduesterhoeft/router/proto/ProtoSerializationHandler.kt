package com.github.mduesterhoeft.router.proto

import com.github.mduesterhoeft.router.ResponseEntity
import com.github.mduesterhoeft.router.SerializationHandler
import com.google.common.net.MediaType
import com.google.protobuf.GeneratedMessageV3
import java.util.Base64

class ProtoSerializationHandler : SerializationHandler {

    private val json = MediaType.parse("application/json")

    override fun supports(acceptHeader: MediaType, response: ResponseEntity<*>): Boolean =
        response.body is GeneratedMessageV3

    override fun serialize(acceptHeader: MediaType, response: ResponseEntity<*>): String {
        val message = response.body as GeneratedMessageV3
        return if (acceptHeader.`is`(json)) {
            ProtoBufUtils.toJsonWithoutWrappers(message)
        } else {
            Base64.getEncoder().encodeToString(message.toByteArray())
        }
    }
}