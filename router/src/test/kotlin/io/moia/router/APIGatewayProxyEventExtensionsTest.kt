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