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

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.net.MediaType
import isCompatibleWith

interface SerializationHandler {
    fun supports(
        acceptHeader: MediaType,
        body: Any,
    ): Boolean

    fun serialize(
        acceptHeader: MediaType,
        body: Any,
    ): String
}

class SerializationHandlerChain(
    private val handlers: List<SerializationHandler>,
) : SerializationHandler {
    override fun supports(
        acceptHeader: MediaType,
        body: Any,
    ): Boolean = handlers.any { it.supports(acceptHeader, body) }

    override fun serialize(
        acceptHeader: MediaType,
        body: Any,
    ): String = handlers.first { it.supports(acceptHeader, body) }.serialize(acceptHeader, body)
}

class JsonSerializationHandler(
    private val objectMapper: ObjectMapper,
) : SerializationHandler {
    private val json = MediaType.parse("application/json")
    private val jsonStructuredSuffixWildcard = MediaType.parse("application/*+json")

    override fun supports(
        acceptHeader: MediaType,
        body: Any,
    ): Boolean = json.isCompatibleWith(acceptHeader) || jsonStructuredSuffixWildcard.isCompatibleWith(acceptHeader)

    override fun serialize(
        acceptHeader: MediaType,
        body: Any,
    ): String = objectMapper.writeValueAsString(body)
}

class PlainTextSerializationHandler(
    val supportedAcceptTypes: List<MediaType> = listOf(MediaType.parse("text/*")),
) : SerializationHandler {
    override fun supports(
        acceptHeader: MediaType,
        body: Any,
    ): Boolean = supportedAcceptTypes.any { acceptHeader.isCompatibleWith(it) }

    override fun serialize(
        acceptHeader: MediaType,
        body: Any,
    ): String = body.toString()
}
