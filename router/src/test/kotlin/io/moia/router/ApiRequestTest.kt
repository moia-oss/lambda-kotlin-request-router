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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Test

class ApiRequestTest {
    @Test
    fun `should match header`() {
        val request = APIGatewayProxyRequestEvent().withHeaders(mapOf("Accept" to "application/json"))

        assertThat(request.acceptHeader()).isEqualTo("application/json")
    }

    @Test
    fun `should match header lowercase`() {
        val request = APIGatewayProxyRequestEvent().withHeaders(mapOf("accept" to "application/json"))

        assertThat(request.acceptHeader()).isEqualTo("application/json")
    }

    @Test
    fun `should match header uppercase`() {
        val request = APIGatewayProxyRequestEvent().withHeaders(mapOf("ACCEPT" to "application/json"))

        assertThat(request.acceptHeader()).isEqualTo("application/json")
    }
}
