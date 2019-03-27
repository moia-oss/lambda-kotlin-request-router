package com.github.mduesterhoeft.router.proto

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.github.mduesterhoeft.router.DeserializationHandler
import com.github.mduesterhoeft.router.contentType
import com.google.common.net.MediaType
import com.google.protobuf.Parser
import java.util.Base64
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.staticFunctions

class ProtoDeserializationHandler : DeserializationHandler {
    private val proto = MediaType.parse("application/x-protobuf")

    override fun supports(input: APIGatewayProxyRequestEvent): Boolean =
        MediaType.parse(input.contentType()).`is`(proto)

    override fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any {
        val bytes = Base64.getDecoder().decode(input.body)
        val parser = (target?.classifier as KClass<*>).staticFunctions.first { it.name == "parser" }.call() as Parser<*>
        return parser.parseFrom(bytes)
    }
}