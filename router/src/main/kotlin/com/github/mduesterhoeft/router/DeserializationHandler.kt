package com.github.mduesterhoeft.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.google.common.net.MediaType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

interface DeserializationHandler {

    fun supports(input: APIGatewayProxyRequestEvent): Boolean

    fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any?
}

class DeserializationHandlerChain(private val handlers: List<DeserializationHandler>) : DeserializationHandler {

    override fun supports(input: APIGatewayProxyRequestEvent): Boolean =
            handlers.any { it.supports(input) }

    override fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any? =
            handlers.firstOrNull { it.supports(input) }?.deserialize(input, target)
}

class JsonDeserializationHandler(val objectMapper: ObjectMapper) : DeserializationHandler {

    private val json = MediaType.parse("application/json")

    override fun supports(input: APIGatewayProxyRequestEvent) =
            input.contentType() != null && MediaType.parse(input.contentType()).`is`(json)

    override fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any? {
        val targetClass = target?.classifier as KClass<*>
        return when {
            targetClass == Unit::class -> Unit
            targetClass == String::class -> input.body!!
            targetClass.isSubclassOf(Collection::class) -> {
                val kClass = target.arguments.first().type!!.classifier as KClass<*>
                val type = TypeFactory.defaultInstance()
                    .constructParametricType(targetClass.javaObjectType, kClass.javaObjectType)
                objectMapper.readValue(input.body, type)
            }
            else -> objectMapper.readValue(input.body, targetClass.java)
        }
    }
}
