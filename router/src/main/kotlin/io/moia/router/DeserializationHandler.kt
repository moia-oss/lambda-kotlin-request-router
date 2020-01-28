/*
 * Copyright 2019 MOIA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.google.common.net.MediaType
import isCompatibleWith
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

interface DeserializationHandler {

    fun supports(input: APIGatewayProxyRequestEvent): Boolean

    fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any?
}

class DeserializationHandlerChain(private val handlers: List<DeserializationHandler>) :
    DeserializationHandler {

    override fun supports(input: APIGatewayProxyRequestEvent): Boolean =
            handlers.any { it.supports(input) }

    override fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any? =
            handlers.firstOrNull { it.supports(input) }?.deserialize(input, target)
}

class JsonDeserializationHandler(private val objectMapper: ObjectMapper) : DeserializationHandler {

    private val json = MediaType.parse("application/json")
    private val jsonStructuredSuffixWildcard = MediaType.parse("application/*+json")

    override fun supports(input: APIGatewayProxyRequestEvent) =
        if (input.contentType() == null)
            false
        else {
            MediaType.parse(input.contentType()).let { json.isCompatibleWith(it) || jsonStructuredSuffixWildcard.isCompatibleWith(it) }
        }

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

object PlainTextDeserializationHandler : DeserializationHandler {
    private val text = MediaType.parse("text/*")
    override fun supports(input: APIGatewayProxyRequestEvent): Boolean =
        if (input.contentType() == null)
            false
        else
            MediaType.parse(input.contentType()).isCompatibleWith(text)

    override fun deserialize(input: APIGatewayProxyRequestEvent, target: KType?): Any? =
        input.body
}
