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
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.isTrue
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import io.moia.router.Router.Companion.router
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Suppress("ktlint:standard:max-line-length")
class RequestHandlerTest {
    private val testRequestHandler = TestRequestHandler()
    private val mapper = testRequestHandler.objectMapper

    @Test
    fun `should match request`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"greeting":"Hello"}""")
    }

    @Test
    fun `should match request with path parameter`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some/me")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"greeting":"Hello me"}""")
    }

    @Test
    fun `should return not acceptable on unsupported accept header`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "image/jpg")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(406)
    }

    @Test
    fun `should return unsupported media type`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some")
                    .withHttpMethod("POST")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "image/jpg",
                        ),
                    ),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(415)
    }

    @Test
    fun `should handle request with body`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{ "greeting": "some" }"""),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"greeting":"some"}""")
    }

    @Test
    fun `should handle request with body as a List`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/somes")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""[{ "greeting": "some" },{ "greeting": "some1" }]""".trimMargin()),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""[{"greeting":"some"},{"greeting":"some1"}]""")
    }

    @Test
    fun `should return method not allowed`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some")
                    .withHttpMethod("PUT")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "image/jpg",
                        ),
                    ),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(405)
    }

    @Test
    fun `should return not found`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some-other")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `should invoke filter chain`() {
        val handler = TestRequestHandlerWithFilter()
        val response =
            handler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(handler.filterInvocations).isEqualTo(2)
    }

    @Test
    fun `should invoke filter chain also for non successful requests`() {
        val handler = TestRequestHandlerWithFilter()
        val response =
            handler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some-internal-server-error")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(500)
        assertThat(response.headers["header"]).isEqualTo("value")
        assertThat(handler.filterInvocations).isEqualTo(2)
    }

    @Test
    fun `should ignore content-type header when handler expects none`() {
        val handler = TestRequestHandlerWithFilter()
        val response =
            handler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some")
                    .withHttpMethod("GET")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "content-type" to "application/json",
                        ),
                    ),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should handle deserialization error`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("{}"),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(422)
    }

    @Test
    fun `should handle deserialization error, when field has invalid format`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{"greeting": "hello","age": "a"}"""),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(422)
        val body = mapper.readValue<List<UnprocessableEntityError>>(response.body)
        assertThat(body.size).isEqualTo(1)
        with(body.first()) {
            assertThat(code).isEqualTo("FIELD")
            assertThat(message).isEqualTo("INVALID_FIELD_FORMAT")
            assertThat(path).isEqualTo("age")
            assertThat(details.isNotEmpty()).isEqualTo(false)
        }
    }

    @Test
    fun `should handle deserialization error, when field can not be parsed to class`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{"greeting": "hello","age": 1, "bday": "2000-01-AA"}"""),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(422)
        val body = mapper.readValue<List<UnprocessableEntityError>>(response.body)
        assertThat(body.size).isEqualTo(1)
        with(body.first()) {
            assertThat(code).isEqualTo("FIELD")
            assertThat(message).isEqualTo("INVALID_FIELD_FORMAT")
            assertThat(path).isEqualTo("bday")
            assertThat(details.isNotEmpty()).isEqualTo(true)
        }
    }

    @Test
    fun `should handle deserialization error, when json can not be parsed`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{"greeting": "hello", bday: "2000-01-01"}"""),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(422)
        val body = mapper.readValue<List<UnprocessableEntityError>>(response.body)
        assertThat(body.size).isEqualTo(1)
        with(body.first()) {
            assertThat(code).isEqualTo("ENTITY")
            assertThat(message).isEqualTo("INVALID_ENTITY")
            assertThat(path).isEqualTo("")
            assertThat(details.isNotEmpty()).isEqualTo(true)
        }
    }

    @Test
    fun `should return 400 on missing body when content type stated`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody(null),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(400)
        assertThat(mapper.readValue<ApiError>(response.body).code).isEqualTo("REQUEST_BODY_MISSING")
    }

    @Test
    fun `should handle null body when content type is stated and request handler body type is nullable`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some-nullable")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody(null),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"greeting":""}""")
    }

    @Test
    fun `should handle api exception`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some-api-exception")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(400)
    }

    @Test
    fun `should handle internal server error`() {
        val response =
            testRequestHandler.handleRequest(
                APIGatewayProxyRequestEvent()
                    .withPath("/some-internal-server-error")
                    .withHttpMethod("GET")
                    .withHeaders(mapOf("Accept" to "application/json")),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(500)
    }

    @Test
    fun `should handle request with a media type range in accept header`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/xhtml+xml, application/json, application/xml;q=0.9, image/webp, */*;q=0.8",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{ "greeting": "some" }"""),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.getHeaderCaseInsensitive("content-type")).isEqualTo("application/json")

        assertThat(response.body).isEqualTo("""{"greeting":"some"}""")
    }

    @Test
    fun `should handle request with accept all header`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "*/*",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{ "greeting": "some" }"""),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.getHeaderCaseInsensitive("content-type")).isEqualTo("application/vnd.moia.v2+json")

        assertThat(response.body).isEqualTo("""{"greeting":"v2"}""")
    }

    @Test
    fun `should handle subtype structured suffix wildcard`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/vnd.moia.v1+json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{ "greeting": "some" }"""),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"greeting":"some"}""")
    }

    @Test
    fun `should match version`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/vnd.moia.v2+json",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{ "greeting": "v2" }"""),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"greeting":"v2"}""")
        assertThat(response.getHeaderCaseInsensitive("content-type")).isEqualTo("application/vnd.moia.v2+json")
    }

    @Test
    fun `should fail with 406 Not Acceptable on an unparsable media type`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "*",
                            "Content-Type" to "application/json",
                        ),
                    ).withBody("""{ "greeting": "some" }"""),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(406)
    }

    @Test
    fun `should match request requiring permission`() {
        val response =
            TestRequestHandlerAuthorization().handleRequest(
                GET("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Authorization" to
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwZXJtaXNzaW9ucyI6InBlcm1pc3Npb24xIn0.E3PxWx68uP2s9yyAV7UVs8egyrGTIuWXjtkcqAA840I",
                        ),
                    ),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should match request requiring permission from custom header`() {
        val response =
            TestRequestHandlerCustomAuthorizationHeader().handleRequest(
                GET("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Custom-Auth" to
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwZXJtaXNzaW9ucyI6InBlcm1pc3Npb24xIn0.E3PxWx68uP2s9yyAV7UVs8egyrGTIuWXjtkcqAA840I",
                        ),
                    ),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should fail on missing permission`() {
        val response =
            TestRequestHandlerAuthorization().handleRequest(
                GET("/some")
                    .withHeaders(
                        mapOf(
                            "Accept" to "application/json",
                            "Authorization" to
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwZXJtaXNzaW9ucyI6InBlcm1pc3Npb24yIn0.RA8ERppuFmastqFN-6C98WqMEE7L6h88WylMeq6jh1w",
                        ),
                    ),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(403)
    }

    @Test
    fun `Request without headers should return status code 406`() {
        val response =
            testRequestHandler.handleRequest(
                GET("/some"),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(406)
    }

    @Test
    fun `Request without request path should return status code 404`() {
        val response =
            testRequestHandler.handleRequest(
                GET(),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `Successful POST request should return status code 204`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/no-content")
                    .withHeader("Accept", "application/json")
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "greeting": "some" }"""),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isNullOrEmpty()
    }

    @Test
    fun `Create should not return a location header`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/create-without-location")
                    .withHeader("Accept", "application/json")
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "greeting": "some" }"""),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(201)
        assertThat(response.headers.containsKey("location")).isFalse()
    }

    @Test
    fun `Create should return a location header`() {
        val response =
            testRequestHandler.handleRequest(
                POST("/create-with-location")
                    .withHeader("Accept", "application/json")
                    .withHeader("Content-Type", "application/json")
                    .withBody("""{ "greeting": "some" }"""),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(201)
        assertThat(response.headers.containsKey("location")).isTrue()
        assertThat(response.headers["location"]).isEqualTo("http://localhost/test")
    }

    @Test
    fun `Deletion should ignore the body and content-type`() {
        val response =
            testRequestHandler.handleRequest(
                DELETE("/delete-me")
                    .withHeader("Accept", "application/json")
                    .withHeader("Content-Type", "text/csv")
                    .withBody("this may be faulty"),
                mockk(),
            )
        assertThat(response.statusCode).isEqualTo(204)
    }

    @Test
    fun `Should handle query parameters successfully`() {
        TestQueryParamParsingHandler().handleRequest(
            GET("/search")
                .withQueryStringParameters(
                    mapOf(
                        "testQueryParam" to "foo",
                    ),
                ).withMultiValueQueryStringParameters(
                    mapOf(
                        "testMultiValueQueryStringParam" to listOf("foo", "bar"),
                    ),
                ),
            mockk(),
        )
        TestQueryParamParsingHandler().handleRequest(
            GET("/search?testQueryParam=foo&testMultiValueQueryStringParam=foo&testMultiValueQueryStringParam=bar"),
            mockk(),
        )
    }

    @Test
    fun `Not existing path parameter should throw an error`() {
        val response =
            testRequestHandler.handleRequest(
                GET("/non-existing-path-parameter")
                    .withHeader("accept", "application/json"),
                mockk(),
            )
        assertEquals(500, response.statusCode)
        assertEquals(
            "{\"message\":\"Could not find path parameter 'foo\",\"code\":\"INTERNAL_SERVER_ERROR\",\"details\":{}}",
            response.body,
        )
    }

    @Test
    fun `should return the content type that is accepted`() {
        val jsonResponse =
            AcceptTypeDependingHandler().handleRequest(
                GET("/all-objects")
                    .withHeader("accept", "application/json"),
                mockk(),
            )
        assertEquals(200, jsonResponse.statusCode)
        assertEquals("application/json", jsonResponse.getHeaderCaseInsensitive("content-type"))
        assertEquals("[{\"text\":\"foo\",\"number\":1},{\"text\":\"bar\",\"number\":2}]", jsonResponse.body)
        val plainTextResponse =
            AcceptTypeDependingHandler().handleRequest(
                GET("/all-objects")
                    .withHeader("accept", "text/plain"),
                mockk(),
            )
        assertEquals(200, plainTextResponse.statusCode)
        assertEquals("text/plain", plainTextResponse.getHeaderCaseInsensitive("content-type"))
        assertEquals("[CustomObject(text=foo, number=1), CustomObject(text=bar, number=2)]", plainTextResponse.body)
    }

    @Test
    fun `headers should be case insensitive`() {
        val request =
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(
                    mapOf(
                        "Accept" to "Application/Json",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0",
                    ),
                )
        val response = testRequestHandler.handleRequest(request, mockk())

        assertThat(request.headers["accept"].toString()).isEqualTo("Application/Json")
        assertThat(request.headers["user-agent"].toString())
            .isEqualTo(
                "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0",
            )
        assertThat(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should deserialize plain text`() {
        class SampleRouter : RequestHandler() {
            override val router =
                router {
                    POST("/some", { r: Request<String> -> ResponseEntity.ok(r.body) })
                        .producing("text/plain")
                        .consuming("text/plain")
                }
        }
        val request =
            POST("/some")
                .withAcceptHeader("text/plain")
                .withContentTypeHeader("text/plain")
                .withBody("just text")

        val response = SampleRouter().handleRequest(request, mockk())

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.getHeaderCaseInsensitive("content-type")).isEqualTo("text/plain")
        assertThat(response.body).isEqualTo("just text")
    }

    @Test
    fun `should be able to use function references as handler`() {
        class DummyHandler : RequestHandler() {
            val dummy =
                object {
                    @Suppress("UNUSED_PARAMETER")
                    fun handler(r: Request<Unit>) = ResponseEntity.ok(Unit)
                }

            override fun exceptionToResponseEntity(ex: Exception) = throw ex

            override val router =
                router {
                    GET("/some", dummy::handler).producing("application/json")
                }
        }

        val response =
            DummyHandler().handleRequest(
                APIGatewayProxyRequestEvent()
                    .withHttpMethod("GET")
                    .withPath("/some")
                    .withAcceptHeader("application/json"),
                mockk(),
            )

        assertThat(response.statusCode).isEqualTo(200)
    }

    class TestRequestHandlerAuthorization : RequestHandler() {
        override val router =
            router {
                GET("/some") { _: Request<Unit> ->
                    ResponseEntity.ok("hello")
                }.requiringPermissions("permission1")
            }

        override fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler =
            {
                JwtPermissionHandler(
                    request = it,
                    permissionsClaim = "permissions",
                    permissionSeparator = ",",
                )
            }
    }

    class TestRequestHandlerCustomAuthorizationHeader : RequestHandler() {
        override val router =
            router {
                GET("/some") { _: Request<Unit> ->
                    ResponseEntity.ok("hello")
                }.requiringPermissions("permission1")
            }

        override fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler =
            {
                JwtPermissionHandler(
                    accessor =
                        JwtAccessor(
                            request = it,
                            authorizationHeaderName = "custom-auth",
                        ),
                    permissionsClaim = "permissions",
                    permissionSeparator = ",",
                )
            }
    }

    class TestRequestHandlerWithFilter : RequestHandler() {
        var filterInvocations = 0

        private val incrementingFilter =
            Filter { next ->
                { request ->
                    filterInvocations += 1
                    next(request).apply { withHeader("header", "value") }
                }
            }
        override val router =
            router {
                filter = incrementingFilter.then(incrementingFilter)

                GET("/some") { _: Request<Unit> ->
                    ResponseEntity.ok("hello")
                }
                GET<Unit, Unit>("/some-internal-server-error") {
                    throw IllegalArgumentException("boom")
                }
            }
    }

    class TestRequestHandler : RequestHandler() {
        data class TestResponse(
            val greeting: String,
        )

        data class TestRequest(
            val greeting: String,
            val age: Int = 0,
            val bday: LocalDate = LocalDate.now(),
        )

        override val router =
            router {
                GET("/some") { _: Request<Unit> ->
                    ResponseEntity.ok(
                        TestResponse(
                            "Hello",
                        ),
                    )
                }
                GET<Unit, Unit>("/some-api-exception") {
                    throw ApiException("boom", "BOOM", 400, mapOf("more" to "info"))
                }
                GET<Unit, Unit>("/some-internal-server-error") {
                    throw IllegalArgumentException("boom")
                }
                GET("/some/{id}") { r: Request<Unit> ->
                    assertThat(r.pathParameters.containsKey("id")).isTrue()
                    ResponseEntity.ok(
                        TestResponse(
                            "Hello ${r.getPathParameter("id")}",
                        ),
                    )
                }

                POST("/some") { _: Request<TestRequest> ->
                    ResponseEntity.ok(
                        TestResponse(
                            "v2",
                        ),
                    )
                }.producing("application/vnd.moia.v2+json")

                POST("/some") { r: Request<TestRequest> ->
                    ResponseEntity.ok(
                        TestResponse(
                            r.body.greeting,
                        ),
                    )
                }.producing("application/json", "application/*+json")

                POST("/some-nullable") { r: Request<TestRequest?> ->
                    ResponseEntity.ok(
                        TestResponse(
                            r.body?.greeting.orEmpty(),
                        ),
                    )
                }.producing("application/json")

                POST("/somes") { r: Request<List<TestRequest>> ->
                    ResponseEntity.ok(
                        r.body
                            .map {
                                TestResponse(
                                    it.greeting,
                                )
                            }.toList(),
                    )
                }
                POST("/no-content") { _: Request<TestRequest> ->
                    ResponseEntity.noContent()
                }
                POST<TestRequest, Unit>("/create-without-location") { _: Request<TestRequest> ->
                    ResponseEntity.created(null, null, emptyMap())
                }
                POST<TestRequest, Unit>("/create-with-location") { r: Request<TestRequest> ->
                    ResponseEntity.created(null, r.apiRequest.location("test"), emptyMap())
                }
                DELETE("/delete-me") { _: Request<Unit> ->
                    ResponseEntity.noContent()
                }
                GET<Unit, Unit>("/non-existing-path-parameter") { request: Request<Unit> ->
                    request.getPathParameter("foo")
                    ResponseEntity.ok(null)
                }
            }
    }

    class TestQueryParamParsingHandler : RequestHandler() {
        override val router =
            router {
                GET<TestRequestHandler.TestRequest, Unit>("/search") { r: Request<TestRequestHandler.TestRequest> ->
                    assertThat(r.getQueryParameter("testQueryParam")).isNotNull()
                    assertThat(r.getQueryParameter("testQueryParam")).isEqualTo("foo")
                    assertThat(r.queryParameters!!["testQueryParam"]).isNotNull()
                    assertThat(r.getMultiValueQueryStringParameter("testMultiValueQueryStringParam")).isNotNull()
                    assertThat(r.getMultiValueQueryStringParameter("testMultiValueQueryStringParam")).isEqualTo(listOf("foo", "bar"))
                    assertThat(r.multiValueQueryStringParameters!!["testMultiValueQueryStringParam"]).isNotNull()
                    ResponseEntity.ok(null)
                }
            }
    }

    class AcceptTypeDependingHandler : RequestHandler() {
        data class CustomObject(
            val text: String,
            val number: Int,
        )

        override val router =
            router {
                defaultConsuming = setOf("application/json", "text/plain")
                defaultProducing = setOf("application/json", "text/plain")
                GET("/all-objects") { _: Request<Unit> ->
                    ResponseEntity.ok(body = listOf(CustomObject("foo", 1), CustomObject("bar", 2)))
                }
            }
    }
}
