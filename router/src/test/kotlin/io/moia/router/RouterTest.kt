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
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.moia.router.Router.Companion.router
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouterTest {
    @Test
    fun `should register get route with default accept header`() {
        val router =
            router {
                GET("/some") { r: Request<Unit> ->
                    ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
                }
            }

        assertThat(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assertThat(method).isEqualTo("GET")
            assertThat(pathPattern).isEqualTo("/some")
            assertThat(consumes).isEmpty()
            assertThat(produces).isEqualTo(setOf("application/json"))
        }
    }

    @Test
    fun `should register routes`() {
        val router =
            router {
                PUT("/some") { _: Request<Unit> -> ResponseEntity.ok("") }
                PATCH("/some") { _: Request<Unit> -> ResponseEntity.ok("") }
                DELETE("/some") { _: Request<Unit> -> ResponseEntity.ok("") }
                POST("/some") { _: Request<Unit> -> ResponseEntity.ok("") }
            }

        then(router.routes.map { it.requestPredicate.method }).containsOnly("PUT", "PATCH", "DELETE", "POST")
    }

    @Test
    fun `should register post route with specific content types`() {
        val router =
            router {
                POST("/some") { r: Request<Unit> ->
                    ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
                }
                    .producing("text/plain")
                    .consuming("text/plain")
            }

        assertThat(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assertThat(method).isEqualTo("POST")
            assertThat(pathPattern).isEqualTo("/some")
            assertThat(consumes).isEqualTo(setOf("text/plain"))
            assertThat(produces).isEqualTo(setOf("text/plain"))
        }
    }

    @Test
    fun `should register get route with custom default content types`() {
        val router =
            router {
                defaultConsuming = setOf("text/plain")
                defaultProducing = setOf("text/plain")

                POST("/some") { r: Request<Unit> ->
                    ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
                }
            }

        assertThat(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assertThat(method).isEqualTo("POST")
            assertThat(pathPattern).isEqualTo("/some")
            assertThat(consumes).isEqualTo(setOf("text/plain"))
            assertThat(produces).isEqualTo(setOf("text/plain"))
        }
    }

    @Test
    fun `should handle greedy path variables successfully`() {
        val router =
            router {
                POST("/some/{proxy+}") { r: Request<Unit> ->
                    ResponseEntity.ok("""{"hello": "world", "request":"${r.body}"}""")
                }
            }
        assertThat(router.routes).hasSize(1)
        with(router.routes.first().requestPredicate) {
            assertTrue(UriTemplate.from(pathPattern).matches("/some/sub/sub/sub/path"))
        }
    }

    @Test
    fun `should not consume for a deletion route`() {
        val router =
            router {
                DELETE<Unit, Unit>("/delete-me") { _: Request<Unit> ->
                    ResponseEntity.ok(null)
                }
            }
        with(router.routes.first().requestPredicate) {
            assertThat(consumes).isEqualTo(setOf<String>())
        }
    }

    @Test
    fun `request should contain ProxyRequestContext`() {
        val claims =
            mapOf(
                "foobar" to "foo",
            )
        val context =
            APIGatewayProxyRequestEvent.ProxyRequestContext().apply {
                authorizer = mapOf("claims" to claims)
            }

        val request =
            Request<Unit>(
                APIGatewayProxyRequestEvent()
                    .withPath("/some-other")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json"))
                    .withRequestContext(context),
                Unit,
            )
        assertThat(request.requestContext.authorizer!!["claims"]).isEqualTo(claims)
    }
}
