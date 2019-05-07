package io.moia.router.proto

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.moia.router.Request
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router
import io.moia.router.bodyAsBytes
import io.moia.router.proto.sample.SampleOuterClass.Sample
import io.mockk.mockk
import io.moia.router.ApiError
import io.moia.router.ApiException
import io.moia.router.GET
import io.moia.router.UnprocessableEntityError
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
        assert(response.isBase64Encoded).isTrue()
    }

    @Test
    fun `should return 406-unacceptable error in proto`() {

        val response = testRequestHandler.handleRequest(
            GET("/some-proto")
                .withHeaders(mapOf(
                    "Accept" to "text/plain"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(406)
        assert(io.moia.router.proto.sample.SampleOuterClass.ApiError.parseFrom(response.bodyAsBytes()).getCode()).isEqualTo("NOT_ACCEPTABLE")
    }

    @Test
    fun `should return api error in protos`() {

        val response = testRequestHandler.handleRequest(
            GET("/some-error")
                .withHeaders(mapOf(
                    "Accept" to "application/x-protobuf"
                )), mockk()
        )

        assert(response.statusCode).isEqualTo(400)
        with(io.moia.router.proto.sample.SampleOuterClass.ApiError.parseFrom(response.bodyAsBytes())) {
            assert(getCode()).isEqualTo("BOOM")
            assert(getMessage()).isEqualTo("boom")
        }
    }

    class TestRequestHandler : ProtoEnabledRequestHandler() {

        override val router = router {
            defaultProducing = setOf("application/x-protobuf")
            defaultConsuming = setOf("application/x-protobuf")

            defaultContentType = "application/x-protobuf"

            GET("/some-proto") { _: Request<Unit> -> ResponseEntity.ok(Sample.newBuilder().setHello("Hello").build()) }
                .producing("application/x-protobuf", "application/json")
            POST("/some-proto") { r: Request<Sample> -> ResponseEntity.ok(r.body) }
            GET<Unit, Unit>("/some-error") { _: Request<Unit> -> throw ApiException("boom", "BOOM", 400) }
        }

        override fun createErrorBody(error: ApiError): Any =
            io.moia.router.proto.sample.SampleOuterClass.ApiError.newBuilder()
                .setMessage(error.message)
                .setCode(error.code)
                .build()

        override fun createUnprocessableEntityErrorBody(errors: List<UnprocessableEntityError>): Any = listOf(
            io.moia.router.proto.sample.SampleOuterClass.UnprocessableEntityError.newBuilder()
                .setMessage(error.message)
                .setCode(error.code)
                .setPath(error.path)
                .build()
        )
    }
}