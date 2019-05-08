package io.moia.router

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNullOrEmpty
import assertk.assertions.isTrue
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.mockk
import io.moia.router.Router.Companion.router
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequestHandlerTest {

    private val testRequestHandler = TestRequestHandler()

    @Test
    fun `should match request`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"Hello"}""")
    }

    @Test
    fun `should match request with path parameter`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some/me")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"Hello me"}""")
    }

    @Test
    fun `should return not acceptable on unsupported accept header`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "img/jpg")), mockk()
        )

        assert(response.statusCode).isEqualTo(406)
    }

    @Test
    fun `should return unsupported media type`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("POST")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "image/jpg"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(415)
    }

    @Test
    fun `should handle request with body`() {

        val response = testRequestHandler.handleRequest(
            POST("/some")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/json"
                ))
                .withBody("""{ "greeting": "some" }"""), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"some"}""")
    }

    @Test
    fun `should handle request with body as a List`() {

        val response = testRequestHandler.handleRequest(
                POST("/somes")
                        .withHeaders(mapOf(
                                "Accept" to "application/json",
                                "Content-Type" to "application/json"
                        ))
                        .withBody("""[{ "greeting": "some" },{ "greeting": "some1" }]""".trimMargin()), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""[{"greeting":"some"},{"greeting":"some1"}]""")
    }

    @Test
    fun `should return method not allowed`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("PUT")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "image/jpg"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(405)
    }

    @Test
    fun `should return not found`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-other")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

        assert(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `should invoke filter chain`() {

        val handler = TestRequestHandlerWithFilter()
        val response = handler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(handler.filterInvocations).isEqualTo(2)
    }

    @Test
    fun `should ignore content-type header when handler expects none`() {

        val handler = TestRequestHandlerWithFilter()
        val response = handler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "content-type" to "application/json"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should handle deserialization error`() {

        val response = testRequestHandler.handleRequest(
            POST("/some")
                .withHeaders(
                    mapOf(
                        "Accept" to "application/json",
                        "Content-Type" to "application/json"
                    )
                )
                .withBody("{}"), mockk()
        )
        assert(response.statusCode).isEqualTo(422)
    }

    @Test
    fun `should handle deserialization error, when field has invalid format`() {

        val response = testRequestHandler.handleRequest(
            POST("/some")
                .withHeaders(
                    mapOf(
                        "Accept" to "application/json",
                        "Content-Type" to "application/json"
                    )
                )
                .withBody("""{"greeting": "hello","age": "a"}"""), mockk()
        )
        assert(response.statusCode).isEqualTo(422)
    }

    @Test
    fun `should handle api exception`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-api-exception")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

        assert(response.statusCode).isEqualTo(400)
    }

    @Test
    fun `should handle internal server error`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-internal-server-error")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

        assert(response.statusCode).isEqualTo(500)
    }

    @Test
    fun `should handle request with a media type range in accept header`() {

        val response = testRequestHandler.handleRequest(
            POST("/some")
                .withHeaders(mapOf(
                    "Accept" to "application/xhtml+xml, application/json, application/xml;q=0.9, image/webp, */*;q=0.8",
                    "Content-Type" to "application/json"
                ))
                .withBody("""{ "greeting": "some" }"""), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(response.getHeaderCaseInsensitive("content-type")).isEqualTo("application/json")

        assert(response.body).isEqualTo("""{"greeting":"some"}""")
    }

    @Test
    fun `should match request requiring permission`() {

        val response = TestRequestHandlerAuthorization().handleRequest(
            GET("/some")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwZXJtaXNzaW9ucyI6InBlcm1pc3Npb24xIn0.E3PxWx68uP2s9yyAV7UVs8egyrGTIuWXjtkcqAA840I"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should match request requiring permission from custom header`() {

        val response = TestRequestHandlerCustomAuthorizationHeader().handleRequest(
            GET("/some")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Custom-Auth" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwZXJtaXNzaW9ucyI6InBlcm1pc3Npb24xIn0.E3PxWx68uP2s9yyAV7UVs8egyrGTIuWXjtkcqAA840I"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should fail on missing permission`() {

        val response = TestRequestHandlerAuthorization().handleRequest(
            GET("/some")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJwZXJtaXNzaW9ucyI6InBlcm1pc3Npb24yIn0.RA8ERppuFmastqFN-6C98WqMEE7L6h88WylMeq6jh1w"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(403)
    }

    @Test
    fun `Request without headers should return status code 406`() {
        val response = testRequestHandler.handleRequest(
            GET("/some"),
            mockk()
        )
        assert(response.statusCode).isEqualTo(406)
    }

    @Test
    fun `Request without request path should return status code 404`() {
        val response = testRequestHandler.handleRequest(
            GET(),
            mockk()
        )
        assert(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `Successful POST request should return status code 204`() {
        val response = testRequestHandler.handleRequest(
            POST("/no-content")
                .withHeader("Accept", "application/json")
                .withHeader("Content-Type", "application/json")
                .withBody("""{ "greeting": "some" }"""),
            mockk()
        )
        assert(response.statusCode).isEqualTo(204)
        assert(response.body).isNullOrEmpty()
    }

    @Test
    fun `Create should not return a location header`() {
        val response = testRequestHandler.handleRequest(
            POST("/create-without-location")
                .withHeader("Accept", "application/json")
                .withHeader("Content-Type", "application/json")
                .withBody("""{ "greeting": "some" }"""),
            mockk()
        )
        assert(response.statusCode).isEqualTo(201)
        assert(response.headers.containsKey("location")).isFalse()
    }

    @Test
    fun `Create should return a location header`() {
        val response = testRequestHandler.handleRequest(
            POST("/create-with-location")
                .withHeader("Accept", "application/json")
                .withHeader("Content-Type", "application/json")
                .withBody("""{ "greeting": "some" }"""),
            mockk()
        )
        assert(response.statusCode).isEqualTo(201)
        assert(response.headers.containsKey("location")).isTrue()
        assert(response.headers["location"]).isEqualTo("http://localhost/test")
    }

    @Test
    fun `Deletion should ignore the body and content-type`() {
        val response = testRequestHandler.handleRequest(
            DELETE("/delete-me")
                .withHeader("Accept", "application/json")
                .withHeader("Content-Type", "text/csv")
                .withBody("this may be faulty"),
            mockk()
        )
        assert(response.statusCode).isEqualTo(204)
    }

    @Test
    fun `Should handle query parameters successfully`() {
        TestQueryParamParsingHandler().handleRequest(
            GET("/search")
                .withQueryStringParameters(mapOf(
                    "testQueryParam" to "foo"
                ))
                .withMultiValueQueryStringParameters(mapOf(
                    "testMultiValueQueryStringParam" to listOf("foo", "bar")
                )),
            mockk()
        )
        TestQueryParamParsingHandler().handleRequest(
            GET("/search?testQueryParam=foo&testMultiValueQueryStringParam=foo&testMultiValueQueryStringParam=bar"),
            mockk()
        )
    }

    @Test
    fun `Not existing path parameter should throw an error`() {
        val response = testRequestHandler.handleRequest(
            GET("/non-existing-path-parameter")
                .withHeader("accept", "application/json"),
            mockk()
        )
        assertEquals(500, response.statusCode)
        assertEquals("{\"message\":\"Could not find path parameter 'foo\",\"code\":\"INTERNAL_SERVER_ERROR\",\"details\":{}}", response.body)
    }

    class TestRequestHandlerAuthorization : RequestHandler() {
        override val router = router {
            GET("/some") { _: Request<Unit> ->
                ResponseEntity.ok("hello")
            }.requiringPermissions("permission1")
        }

        override fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler =
            { JwtPermissionHandler(
                request = it,
                permissionsClaim = "permissions",
                permissionSeparator = ","
            ) }
    }

    class TestRequestHandlerCustomAuthorizationHeader : RequestHandler() {
        override val router = router {
            GET("/some") { _: Request<Unit> ->
                ResponseEntity.ok("hello")
            }.requiringPermissions("permission1")
        }

        override fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler =
            { JwtPermissionHandler(
                accessor = JwtAccessor(
                    request = it,
                    authorizationHeaderName = "custom-auth"),
                permissionsClaim = "permissions",
                permissionSeparator = ","
            ) }
    }

    class TestRequestHandlerWithFilter : RequestHandler() {

        var filterInvocations = 0

        private val incrementingFilter = Filter { next ->
            { request ->
                filterInvocations += 1
                next(request)
            }
        }
        override val router = router {
            filter = incrementingFilter.then(incrementingFilter)

            GET("/some") { _: Request<Unit> ->
                ResponseEntity.ok("hello")
            }
        }
    }

    class TestRequestHandler : RequestHandler() {

        data class TestResponse(val greeting: String)
        data class TestRequest(val greeting: String, val age: Int = 0)

        override val router = router {
            GET("/some") { _: Request<Unit> ->
                ResponseEntity.ok(
                    TestResponse(
                        "Hello"
                    )
                )
            }
            GET<Unit, Unit>("/some-api-exception") {
                throw ApiException("boom", "BOOM", 400, mapOf("more" to "info"))
            }
            GET<Unit, Unit>("/some-internal-server-error") {
                throw IllegalArgumentException("boom")
            }
            GET("/some/{id}") { r: Request<Unit> ->
                assert(r.pathParameters.containsKey("id")).isTrue()
                ResponseEntity.ok(
                    TestResponse(
                        "Hello ${r.getPathParameter("id")}"
                    )
                )
            }
            POST("/some") { r: Request<TestRequest> ->
                ResponseEntity.ok(
                    TestResponse(
                        r.body.greeting
                    )
                )
            }
            POST("/somes") { r: Request<List<TestRequest>> ->
                ResponseEntity.ok(r.body.map {
                    TestResponse(
                        it.greeting
                    )
                }.toList())
            }
            POST("/no-content") { _: Request<TestRequest> ->
                ResponseEntity.noContent()
            }
            POST("/create-without-location") { _: Request<TestRequest> ->
                ResponseEntity.created(null, null, emptyMap())
            }
            POST("/create-with-location") { r: Request<TestRequest> ->
                ResponseEntity.created(null, r.apiRequest.location("test"), emptyMap())
            }
            DELETE("/delete-me") { _: Request<Unit> ->
                ResponseEntity.noContent()
            }
            GET("/non-existing-path-parameter") { request: Request<Unit> ->
                request.getPathParameter("foo")
                ResponseEntity.ok(null)
            }
        }
    }

    class TestQueryParamParsingHandler : RequestHandler() {

        override val router = router {
            GET("/search") { r: Request<TestRequestHandler.TestRequest> ->
                assert(r.getQueryParameter("testQueryParam")).isNotNull()
                assert(r.getQueryParameter("testQueryParam")).isEqualTo("foo")
                assert(r.queryParameters!!["testQueryParam"]).isNotNull()
                assert(r.getMultiValueQueryStringParameter("testMultiValueQueryStringParam")).isNotNull()
                assert(r.getMultiValueQueryStringParameter("testMultiValueQueryStringParam")).isEqualTo(listOf("foo", "bar"))
                assert(r.multiValueQueryStringParameters!!["testMultiValueQueryStringParam"]).isNotNull()
                ResponseEntity.ok(null)
            }
        }
    }
}