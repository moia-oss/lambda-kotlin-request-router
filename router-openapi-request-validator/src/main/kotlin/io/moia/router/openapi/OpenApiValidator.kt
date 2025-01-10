package io.moia.router.openapi

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.Response
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport
import org.slf4j.LoggerFactory

class OpenApiValidator(
    val specUrlOrPayload: String,
) {
    val validator = OpenApiInteractionValidator.createFor(specUrlOrPayload).build()

    fun validate(
        request: APIGatewayProxyRequestEvent,
        response: APIGatewayProxyResponseEvent,
    ): ValidationReport =
        validator
            .validate(request.toRequest(), response.toResponse())
            .also { if (it.hasErrors()) log.error("error validating request and response against $specUrlOrPayload - $it") }

    fun assertValid(
        request: APIGatewayProxyRequestEvent,
        response: APIGatewayProxyResponseEvent,
    ) = validate(request, response).let {
        if (it.hasErrors()) {
            throw ApiInteractionInvalid(
                specUrlOrPayload,
                request,
                response,
                it,
            )
        }
    }

    fun assertValidRequest(request: APIGatewayProxyRequestEvent) =
        validator.validateRequest(request.toRequest()).let {
            if (it.hasErrors()) {
                throw ApiInteractionInvalid(
                    spec = specUrlOrPayload,
                    request = request,
                    validationReport = it,
                )
            }
        }

    fun assertValidResponse(
        request: APIGatewayProxyRequestEvent,
        response: APIGatewayProxyResponseEvent,
    ) = request.toRequest().let { r ->
        validator.validateResponse(r.path, r.method, response.toResponse()).let {
            if (it.hasErrors()) {
                throw ApiInteractionInvalid(
                    spec = specUrlOrPayload,
                    request = request,
                    validationReport = it,
                )
            }
        }
    }

    class ApiInteractionInvalid(
        val spec: String,
        val request: APIGatewayProxyRequestEvent,
        val response: APIGatewayProxyResponseEvent? = null,
        val validationReport: ValidationReport,
    ) : RuntimeException("Error validating request and response against $spec - $validationReport")

    private fun APIGatewayProxyRequestEvent.toRequest(): Request {
        val builder =
            when (httpMethod.toLowerCase()) {
                "get" -> SimpleRequest.Builder.get(path)
                "post" -> SimpleRequest.Builder.post(path)
                "put" -> SimpleRequest.Builder.put(path)
                "patch" -> SimpleRequest.Builder.patch(path)
                "delete" -> SimpleRequest.Builder.delete(path)
                "options" -> SimpleRequest.Builder.options(path)
                "head" -> SimpleRequest.Builder.head(path)
                else -> throw IllegalArgumentException("Unsupported method $httpMethod")
            }
        headers?.forEach { builder.withHeader(it.key, it.value) }
        queryStringParameters?.forEach { builder.withQueryParam(it.key, it.value) }
        builder.withBody(body)
        return builder.build()
    }

    private fun APIGatewayProxyResponseEvent.toResponse(): Response =
        SimpleResponse.Builder
            .status(statusCode)
            .withBody(body)
            .also { headers.forEach { h -> it.withHeader(h.key, h.value) } }
            .build()

    companion object {
        val log = LoggerFactory.getLogger(OpenApiValidator::class.java)
    }
}
