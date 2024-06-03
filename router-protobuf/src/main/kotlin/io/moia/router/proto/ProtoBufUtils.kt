package io.moia.router.proto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.util.JsonFormat

object ProtoBufUtils {
    fun toJsonWithoutWrappers(proto: GeneratedMessageV3): String {
        val message = JsonFormat.printer().omittingInsignificantWhitespace().includingDefaultValueFields().print(proto)
        return removeWrapperObjects(message)
    }

    fun removeWrapperObjects(json: String): String {
        return removeWrapperObjects(
            jacksonObjectMapper().readTree(json),
        ).toString()
    }

    fun removeWrapperObjects(json: JsonNode): JsonNode {
        if (json.isArray) {
            return removeWrapperObjects(json as ArrayNode)
        } else if (json.isObject) {
            if (json.has("value") && json.size() == 1) {
                return json.get("value")
            }
            return removeWrapperObjects(json as ObjectNode)
        }
        return json
    }

    private fun removeWrapperObjects(json: ObjectNode): ObjectNode {
        val result = jacksonObjectMapper().createObjectNode()
        for (entry in json.fields()) {
            if (entry.value.isContainerNode && entry.value.size() > 0) {
                if (entry.value.size() > 0) {
                    result.replace(
                        entry.key,
                        removeWrapperObjects(entry.value),
                    )
                }
            } else {
                result.replace(entry.key, entry.value)
            }
        }
        return result
    }

    private fun removeWrapperObjects(json: ArrayNode): ArrayNode {
        val result = jacksonObjectMapper().createArrayNode()
        for (entry in json) {
            result.add(removeWrapperObjects(entry))
        }
        return result
    }
}
