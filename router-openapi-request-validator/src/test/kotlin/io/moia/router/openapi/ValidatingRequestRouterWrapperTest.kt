package io.moia.router.openapi

import io.mockk.mockk
import io.moia.router.GET
import io.moia.router.Request
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router
import io.moia.router.withAcceptHeader
import org.assertj.core.api.BDDAssertions.then
import org.assertj.core.api.BDDAssertions.thenThrownBy
import org.junit.jupiter.api.Test

class ValidatingRequestRouterWrapperTest {

    @Test
    fun `should return response on successful validation`() {
        val response = ValidatingRequestRouterWrapper(TestRequestHandler(), "openapi.yml")
            .handleRequest(GET("/tests").withAcceptHeader("application/json"), mockk())

        then(response.statusCode).isEqualTo(200)
    }

    @Test
    fun `should fail on response validation error`() {
        thenThrownBy {
            ValidatingRequestRouterWrapper(InvalidTestRequestHandler(), "openapi.yml")
                .handleRequest(GET("/tests").withAcceptHeader("application/json"), mockk())
        }
            .isInstanceOf(OpenApiValidator.ApiInteractionInvalid::class.java)
            .hasMessageContaining("Response status 404 not defined for path")
    }

    @Test
    fun `should fail on request validation error`() {
        thenThrownBy {
            ValidatingRequestRouterWrapper(InvalidTestRequestHandler(), "openapi.yml")
                .handleRequest(GET("/path-not-documented").withAcceptHeader("application/json"), mockk())
        }
            .isInstanceOf(OpenApiValidator.ApiInteractionInvalid::class.java)
            .hasMessageContaining("No API path found that matches request")
    }

    @Test
    fun `should skip validation`() {
        val response = ValidatingRequestRouterWrapper(InvalidTestRequestHandler(), "openapi.yml")
            .handleRequestSkippingRequestAndResponseValidation(GET("/path-not-documented").withAcceptHeader("application/json"), mockk())
        then(response.statusCode).isEqualTo(404)
    }

    @Test
    fun `should apply additional request validation`() {
        thenThrownBy {
            ValidatingRequestRouterWrapper(
                delegate = OpenApiValidatorTest.TestRequestHandler(),
                specUrlOrPayload = "openapi.yml",
                additionalRequestValidationFunctions = listOf({ _ -> throw RequestValidationFailedException() })
            )
                .handleRequest(GET("/tests").withAcceptHeader("application/json"), mockk())
        }
            .isInstanceOf(RequestValidationFailedException::class.java)
    }

    @Test
    fun `should apply additional response validation`() {
        thenThrownBy {
            ValidatingRequestRouterWrapper(
                delegate = OpenApiValidatorTest.TestRequestHandler(),
                specUrlOrPayload = "openapi.yml",
                additionalResponseValidationFunctions = listOf({ _, _ -> throw ResponseValidationFailedException() })
            )
                .handleRequest(GET("/tests").withAcceptHeader("application/json"), mockk())
        }
            .isInstanceOf(ResponseValidationFailedException::class.java)
    }

    private class RequestValidationFailedException : RuntimeException("request validation failed")
    private class ResponseValidationFailedException : RuntimeException("request validation failed")

    private class TestRequestHandler : RequestHandler() {
        override val router = router {
            GET("/tests") { _: Request<Unit> ->
                ResponseEntity.ok("""{"name": "some"}""")
            }
        }
    }

    private class InvalidTestRequestHandler : RequestHandler() {
        override val router = router {
            GET("/tests") { _: Request<Unit> ->
                ResponseEntity.notFound(Unit)
            }
        }
    }
}
