package com.github.mduesterhoeft.router

import assertk.assert
import assertk.assertions.isEqualTo
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
        override val router = Router.router {
            GET("/some") {
               ResponseEntity.ok(TestResponse("Hello"))
            }
            POST("/some") {
                ResponseEntity.ok(TestResponse("Hello post"))
            }
        }
    }
}