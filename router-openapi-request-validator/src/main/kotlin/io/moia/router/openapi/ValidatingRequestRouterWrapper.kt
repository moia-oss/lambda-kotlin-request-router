package io.moia.router.openapi

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.moia.router.RequestHandler
import org.slf4j.LoggerFactory

/**
 * A wrapper around a [io.moia.router.RequestHandler] that transparently validates every request/response against the OpenAPI spec.
 *
 * This can be used in tests to make sure the actual requests and responses match the API specification.
 *
 * It uses [OpenApiValidator] to do the validation.
 *
 * @property delegate the actual [io.moia.router.RequestHandler] to forward requests to.
 * @property specFile the location of the OpenAPI / Swagger specification to use in the validator, or the inline specification to use. See also [com.atlassian.oai.validator.OpenApiInteractionValidator.createFor]]
 */
class ValidatingRequestRouterWrapper(
    val delegate: RequestHandler,
    specUrlOrPayload: String,
    private val additionalRequestValidationFunctions: List<(APIGatewayProxyRequestEvent) -> Unit> = emptyList(),
    private val additionalResponseValidationFunctions: List<(APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent) -> Unit> = emptyList()
) {
    private val openApiValidator = OpenApiValidator(specUrlOrPayload)

    fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
        handleRequest(input = input, context = context, skipRequestValidation = false, skipResponseValidation = false)

    fun handleRequestSkippingRequestAndResponseValidation(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
        handleRequest(input = input, context = context, skipRequestValidation = true, skipResponseValidation = true)

    private fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context, skipRequestValidation: Boolean, skipResponseValidation: Boolean): APIGatewayProxyResponseEvent {

        if (!skipRequestValidation) {
            try {
                openApiValidator.assertValidRequest(input)
                runAdditionalRequestValidations(input)
            } catch (e: Exception) {
                log.error("Validation failed for request $input", e)
                throw e
            }
        }
        val response = delegate.handleRequest(input, context)
        if (!skipResponseValidation) {
            try {
                runAdditionalResponseValidations(input, response)
                openApiValidator.assertValidResponse(input, response)
            } catch (e: Exception) {
                log.error("Validation failed for response $response", e)
                throw e
            }
        }

        return response
    }

    private fun runAdditionalRequestValidations(requestEvent: APIGatewayProxyRequestEvent) {
        additionalRequestValidationFunctions.forEach { it(requestEvent) }
    }

    private fun runAdditionalResponseValidations(requestEvent: APIGatewayProxyRequestEvent, responseEvent: APIGatewayProxyResponseEvent) {
        additionalResponseValidationFunctions.forEach { it(requestEvent, responseEvent) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ValidatingRequestRouterWrapper::class.java)
    }
}