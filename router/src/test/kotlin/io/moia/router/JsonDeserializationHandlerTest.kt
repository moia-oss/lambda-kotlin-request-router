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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonDeserializationHandlerTest {

    val deserializationHandler = JsonDeserializationHandler(jacksonObjectMapper())

    @Test
    fun `should support json`() {
        assertTrue(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "application/json")))
        assertTrue(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "application/vnd.moia.v1+json")))
    }

    @Test
    fun `should not support anything else than json`() {
        assertFalse(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "image/png")))
        assertFalse(deserializationHandler.supports(APIGatewayProxyRequestEvent()
            .withHeader("content-type", "text/plain")))
    }
}