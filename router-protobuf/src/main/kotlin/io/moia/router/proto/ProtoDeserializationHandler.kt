package io.moia.router.proto

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.google.common.net.MediaType
import com.google.protobuf.Parser
import io.moia.router.DeserializationHandler
import io.moia.router.contentType
import isCompatibleWith
import java.util.Base64
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.staticFunctions

class ProtoDeserializationHandler : DeserializationHandler {
    private val proto = MediaType.parse("application/x-protobuf")
    private val protoStructuredSuffixWildcard = MediaType.parse("application/*+x-protobuf")

    override fun supports(input: APIGatewayProxyRequestEvent): Boolean =
        if (input.contentType() == null) {
            false
        } else {
            MediaType.parse(input.contentType()).let { proto.isCompatibleWith(it) || protoStructuredSuffixWildcard.isCompatibleWith(it) }
        }

    override fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any {
        val bytes = Base64.getDecoder().decode(input.body)
        val parser = (target?.classifier as KClass<*>).staticFunctions.first { it.name == "parser" }.call() as Parser<*>
        return parser.parseFrom(bytes)
    }
}
