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
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class APIGatewayProxyEventExtensionsTest {

    @Test
    fun `should add location header`() {

        val request = GET()
            .withHeader("Host", "example.com")
            .withHeader("X-Forwarded-Proto", "http")
            .withHeader("X-Forwarded-Port", "8080")

        val response = APIGatewayProxyResponseEvent()
            .withLocationHeader(request, "/some/path")

        then(response.location()).isEqualTo("http://example.com:8080/some/path")
    }

    @Test
    fun `should add location header with default host and proto and without port`() {

        val request = GET()

        val response = APIGatewayProxyResponseEvent()
            .withLocationHeader(request, "/some/path")

        then(response.location()).isEqualTo("http://localhost/some/path")
    }

    @Test
    fun `should omit default https port`() {

        val request = GET()
            .withHeader("Host", "example.com")
            .withHeader("X-Forwarded-Proto", "https")
            .withHeader("X-Forwarded-Port", "443")

        val location = request.location("some/path")

        then(location.toString()).isEqualTo("https://example.com/some/path")
    }

    @Test
    fun `should omit default http port`() {

        val request = GET()
            .withHeader("Host", "example.com")
            .withHeader("X-Forwarded-Proto", "http")
            .withHeader("X-Forwarded-Port", "80")

        val location = request.location("/some/path")

        then(location.toString()).isEqualTo("http://example.com/some/path")
    }

    @Test
    fun `header class should work as expected with APIGatewayProxyRequestEvent`() {

        val request = APIGatewayProxyRequestEvent().withHeader(Header("foo", "bar"))

        then(request.headers["foo"]).isEqualTo("bar")
    }

    @Test
    fun `header class should work as expected with APIGatewayProxyResponseEvent`() {

        val request = APIGatewayProxyResponseEvent().withHeader(Header("foo", "bar"))

        then(request.headers["foo"]).isEqualTo("bar")
    }
}