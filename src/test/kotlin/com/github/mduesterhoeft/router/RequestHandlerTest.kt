package com.github.mduesterhoeft.router

import assertk.assert
import assertk.assertions.isEqualTo
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.github.mduesterhoeft.router.Router.Companion.router
import com.github.mduesterhoeft.router.sample.proto.SampleOuterClass.Sample
import io.mockk.mockk
import org.junit.jupiter.api.Test

class RequestHandlerTest {

    val testRequestHandler = TestRequestHandler()

    @Test
    fun `should match request`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )!!

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
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"Hello me"}""")
    }

    @Test
    fun `should match request to proto handler and return json`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-proto")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"hello":"Hello","request":""}""")
    }

    @Test
    fun `should match request to proto handler and return proto`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-proto")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/x-protobuf")), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(Sample.parseFrom(response.bodyAsBytes())).isEqualTo(Sample.newBuilder().setHello("Hello").setRequest("").build())
    }

    @Test
    fun `should return not acceptable on unsupported accept header`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "img/jpg")), mockk()
        )!!

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
        )!!

        assert(response.statusCode).isEqualTo(415)
    }

    @Test
    fun `should handle request with body`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some")
                .withHttpMethod("POST")
                .withHeaders(mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/json"
                ))
                .withBody("""{ "greeting": "some" }"""), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"some"}""")
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
        )!!

        assert(response.statusCode).isEqualTo(405)
    }

    @Test
    fun `should return not found`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-other")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )!!

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
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(handler.filterInvocations).isEqualTo(2)
    }

    class TestRequestHandlerWithFilter : RequestHandler() {

        var filterInvocations = 0

        private val incrementingFilter = Filter {
            next -> {
                request ->
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
        data class TestRequest(val greeting: String)

        override val router = router {
            GET("/some") { _: Request<Unit> ->
                ResponseEntity.ok(TestResponse("Hello"))
            }
            GET("/some/{id}") { r: Request<Unit> ->
                ResponseEntity.ok(TestResponse("Hello ${UriTemplate.from("/some/{id}").extract(r.apiRequest.path)["id"]}"))
            }
            GET("/some-proto") { _: Request<Unit> ->
                ResponseEntity.ok(Sample.newBuilder().setHello("Hello").build())
            }
            POST("/some") { r: Request<TestRequest> ->
                ResponseEntity.ok(TestResponse(r.body.greeting))
            }
        }
    }
}