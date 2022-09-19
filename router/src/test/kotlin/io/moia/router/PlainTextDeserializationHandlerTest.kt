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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlainTextDeserializationHandlerTest {

    @Test
    fun `should support text`() {
        assertTrue(
            PlainTextDeserializationHandler.supports(
                APIGatewayProxyRequestEvent()
                    .withHeader("content-type", "text/plain")
            )
        )
        assertTrue(
            PlainTextDeserializationHandler.supports(
                APIGatewayProxyRequestEvent()
                    .withHeader("content-type", "text/csv")
            )
        )
    }

    @Test
    fun `should not support anything else than text`() {
        assertFalse(
            PlainTextDeserializationHandler.supports(
                APIGatewayProxyRequestEvent()
                    .withHeader("content-type", "image/png")
            )
        )
        assertFalse(
            PlainTextDeserializationHandler.supports(
                APIGatewayProxyRequestEvent()
                    .withHeader("content-type", "application/json")
            )
        )
    }

    @Test
    fun `should not support anything when content type is null`() {
        assertFalse(PlainTextDeserializationHandler.supports(APIGatewayProxyRequestEvent()))
    }

    @Test
    fun `should return body`() {
        val request = APIGatewayProxyRequestEvent().withBody("some")
        val result = PlainTextDeserializationHandler.deserialize(request, null)

        assertEquals(request.body, result)
    }
}
