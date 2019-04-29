package io.moia.router.proto

import assertk.assert
import assertk.assertions.isEqualTo
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.moia.router.Request
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router
import io.moia.router.bodyAsBytes
import io.moia.router.proto.sample.SampleOuterClass.Sample
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.Base64

class RequestHandlerTest {

    private val testRequestHandler = TestRequestHandler()

    @Test
    fun `should match request to proto handler and return json`() {

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-proto")
                .withHttpMethod("GET")
                .withHeaders(mapOf("Accept" to "application/json")), mockk()
        )

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
        )

        assert(response.statusCode).isEqualTo(200)
        assert(Sample.parseFrom(response.bodyAsBytes())).isEqualTo(Sample.newBuilder().setHello("Hello").setRequest("").build())
    }

    @Test
    fun `should match request to proto handler and deserialize and return proto`() {

        val request = Sample.newBuilder().setHello("Hello").setRequest("").build()

        val response = testRequestHandler.handleRequest(
            APIGatewayProxyRequestEvent()
                .withPath("/some-proto")
                .withHttpMethod("POST")
                .withBody(Base64.getEncoder().encodeToString(request.toByteArray()))
                .withHeaders(mapOf(
                    "Accept" to "application/x-protobuf",
                    "Content-Type" to "application/x-protobuf"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(200)
        assert(Sample.parseFrom(response.bodyAsBytes())).isEqualTo(request)
    }

    class TestRequestHandler : ProtoEnabledRequestHandler() {

        override val router = router {
            defaultProducing = setOf("application/x-protobuf")
            defaultConsuming = setOf("application/x-protobuf")

            GET("/some-proto") { _: Request<Unit> ->
                ResponseEntity.ok(Sample.newBuilder().setHello("Hello").build())
            }.producing("application/x-protobuf", "application/json")
            POST("/some-proto") { r: Request<Sample> ->
                ResponseEntity.ok(r.body)
            }
        }
    }
}