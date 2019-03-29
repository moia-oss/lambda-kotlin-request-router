package io.moia.router

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.net.MediaType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.jvm.reflect

abstract class RequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    open val objectMapper = jacksonObjectMapper()

    abstract val router: Router

    private val serializationHandlerChain by lazy { SerializationHandlerChain(serializationHandlers()) }
    private val deserializationHandlerChain by lazy { DeserializationHandlerChain(deserializationHandlers()) }

    @Suppress("UNCHECKED_CAST")
    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        log.debug("handling request with method '${input.httpMethod}' and path '${input.path}' - Accept:${input.acceptHeader()} Content-Type:${input.contentType()} $input")
        val routes = router.routes as List<RouterFunction<Any, Any>>
        val matchResults: List<RequestMatchResult> = routes.map { routerFunction: RouterFunction<Any, Any> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            log.debug("match result for route '$routerFunction' is '$matchResult'")
            if (matchResult.match) {
                val handler: HandlerFunction<Any, Any> = routerFunction.handler
                return try {
                    val requestBody = deserializeRequest(handler, input)
                    val request =
                        Request(input, requestBody, routerFunction.requestPredicate.pathPattern)
                    val response = router.filter.then(handler as HandlerFunction<*, *>).invoke(request)
                    createResponse(input, response)
                } catch (e: Exception) {
                    when (e) {
                        is ApiException -> createApiExceptionErrorResponse(input, e)
                            .also { log.info("Caught api error while handling ${input.httpMethod} ${input.path} - $e") }
                        else -> createUnexpectedErrorResponse(input, e)
                            .also { log.error("Caught exception handling ${input.httpMethod} ${input.path} - $e", e) }
                    }
                }
            }

            matchResult
        }
        return handleNonDirectMatch(matchResults, input)
    }

    open fun serializationHandlers(): List<SerializationHandler> = listOf(
        JsonSerializationHandler(
            objectMapper
        )
    )
    open fun deserializationHandlers(): List<DeserializationHandler> = listOf(
        JsonDeserializationHandler(
            objectMapper
        )
    )

    private fun deserializeRequest(
        handler: HandlerFunction<Any, Any>,
        input: APIGatewayProxyRequestEvent
    ): Any? {
        val requestType = handler.reflect()!!.parameters.first().type.arguments.first().type
        return when {
            requestType?.classifier as KClass<*> == Unit::class -> Unit
            else -> deserializationHandlerChain.deserialize(input, requestType)
        }
    }

    private fun handleNonDirectMatch(matchResults: List<RequestMatchResult>, input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        // no direct match
        if (matchResults.any { it.matchPath && it.matchMethod && !it.matchContentType }) {
            return createApiExceptionErrorResponse(
                input, ApiException(
                    httpResponseStatus = 415,
                    message = "Unsupported Media Type",
                    code = "UNSUPPORTED_MEDIA_TYPE"
                )
            )
        }
        if (matchResults.any { it.matchPath && it.matchMethod && !it.matchAcceptType }) {
            return createApiExceptionErrorResponse(
                input, ApiException(
                    httpResponseStatus = 406,
                    message = "Not Acceptable",
                    code = "NOT_ACCEPTABLE"
                )
            )
        }
        if (matchResults.any { it.matchPath && !it.matchMethod }) {
            return createApiExceptionErrorResponse(
                input, ApiException(
                    httpResponseStatus = 405,
                    message = "Method Not Allowed",
                    code = "METHOD_NOT_ALLOWED"
                )
            )
        }
        return createApiExceptionErrorResponse(
            input, ApiException(
                httpResponseStatus = 404,
                message = "Not found",
                code = "NOT_FOUND"
            )
        )
    }

    open fun createApiExceptionErrorResponse(input: APIGatewayProxyRequestEvent, ex: ApiException): APIGatewayProxyResponseEvent =
        APIGatewayProxyResponseEvent()
            .withBody(objectMapper.writeValueAsString(mapOf(
                "message" to ex.message,
                "code" to ex.code,
                "details" to ex.details
            )))
            .withStatusCode(ex.httpResponseStatus)
            .withHeaders(mapOf("Content-Type" to "application/json"))

    open fun createUnexpectedErrorResponse(input: APIGatewayProxyRequestEvent, ex: Exception): APIGatewayProxyResponseEvent =
        when (ex) {
            is MissingKotlinParameterException ->
                APIGatewayProxyResponseEvent()
                    .withBody(objectMapper.writeValueAsString(
                        listOf(mapOf(
                            "path" to ex.parameter.name.orEmpty(),
                            "message" to "Missing required field",
                            "code" to "MISSING_REQUIRED_FIELDS"
                ))))
                .withStatusCode(422)
                .withHeaders(mapOf("Content-Type" to "application/json"))
            else ->
                APIGatewayProxyResponseEvent()
                    .withBody(objectMapper.writeValueAsString(mapOf(
                        "message" to ex.message,
                        "code" to "INTERNAL_SERVER_ERROR"
                    )))
                    .withStatusCode(500)
                    .withHeaders(mapOf("Content-Type" to "application/json"))
        }

    open fun <T> createResponse(input: APIGatewayProxyRequestEvent, response: ResponseEntity<T>): APIGatewayProxyResponseEvent {
        // TODO add default accept type
        val accept = MediaType.parse(input.acceptHeader())
        return when {
            response.body is Unit -> APIGatewayProxyResponseEvent()
                .withStatusCode(204)
                .withHeaders(response.headers)
            serializationHandlerChain.supports(accept, response) ->
                APIGatewayProxyResponseEvent()
                    .withStatusCode(response.statusCode)
                    .withBody(serializationHandlerChain.serialize(accept, response))
                    .withHeaders(response.headers + ("Content-Type" to accept.toString()))
            else -> throw IllegalArgumentException("unsupported response $response")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestHandler::class.java)
    }
}