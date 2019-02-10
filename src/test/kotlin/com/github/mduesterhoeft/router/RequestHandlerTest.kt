package com.github.mduesterhoeft.router

import assertk.assert
import assertk.assertions.isEqualTo
import com.github.mduesterhoeft.router.Router.Companion.router
import com.github.mduesterhoeft.router.sample.proto.SampleOuterClass.Sample
import io.mockk.mockk
import org.junit.jupiter.api.Test

class RequestHandlerTest {

    val testRequestHandler = TestRequestHandler()

    @Test
    fun `should match request`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some",
                httpMethod = "GET",
                headers = mutableMapOf(
                    "Accept" to "application/json"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"Hello"}""")
    }

    @Test
    fun `should match request to proto handler and return json`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some-proto",
                httpMethod = "GET",
                headers = mutableMapOf(
                    "Accept" to "application/json"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"hello":"Hello","request":""}""")
    }

    @Test
    fun `should match request to proto handler and return proto`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some-proto",
                httpMethod = "GET",
                headers = mutableMapOf(
                    "Accept" to "application/x-protobuf"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(Sample.parseFrom(response.body as ByteArray)).isEqualTo(Sample.newBuilder().setHello("Hello").setRequest("").build())
    }

    @Test
    fun `should return not acceptable on unsupported accept header`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some",
                httpMethod = "GET",
                headers = mutableMapOf(
                    "Accept" to "image/jpg"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(406)
    }

    @Test
    fun `should return unsupported media type`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some",
                httpMethod = "POST",
                headers = mutableMapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "image/jpg"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(415)
    }

    @Test
    fun `should handle request with body`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some",
                httpMethod = "POST",
                headers = mutableMapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/json"
                ),
                body = """{ "greeting": "some" }"""
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(200)
        assert(response.body).isEqualTo("""{"greeting":"some"}""")
    }

    @Test
    fun `should return method not allowed`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some",
                httpMethod = "PUT",
                headers = mutableMapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "image/jpg"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(405)
    }

    @Test
    fun `should return not found`() {

        val response = testRequestHandler.handleRequest(
            ApiRequest(
                path = "/some-other",
                httpMethod = "GET",
                headers = mutableMapOf(
                    "Accept" to "application/json"
                )
            ), mockk()
        )!!

        assert(response.statusCode).isEqualTo(404)
    }

    class TestRequestHandler : RequestHandler() {

        data class TestResponse(val greeting: String)
        data class TestRequest(val greeting: String)
        override val router = router {
            GET("/some") { request: Request<Unit> ->
                ResponseEntity.ok(TestResponse("Hello"))
            }
            GET("/some-proto") { request: Request<Unit> ->
                ResponseEntity.ok(Sample.newBuilder().setHello("Hello").build())
            }
            POST("/some") { r: Request<TestRequest> ->
                ResponseEntity.ok(TestResponse(r.body.greeting))
            }
        }
    }
}