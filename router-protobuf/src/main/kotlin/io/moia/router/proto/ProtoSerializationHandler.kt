package io.moia.router.proto

import com.google.protobuf.GeneratedMessageV3
import io.moia.router.ResponseEntity
import io.moia.router.SerializationHandler
import org.apache.http.entity.ContentType
import java.util.Base64

class ProtoSerializationHandler : SerializationHandler {

    private val json = ContentType.parse("application/json")

    override fun supports(acceptHeader: ContentType, response: ResponseEntity<*>): Boolean =
        response.body is GeneratedMessageV3

    override fun serialize(acceptHeader: ContentType, response: ResponseEntity<*>): String {
        val message = response.body as GeneratedMessageV3
        return if (json.mimeType == acceptHeader.mimeType) {
            ProtoBufUtils.toJsonWithoutWrappers(message)
        } else {
            Base64.getEncoder().encodeToString(message.toByteArray())
        }
    }
}