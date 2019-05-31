package io.moia.router

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.net.MediaType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.jvm.reflect

@Suppress("UnstableApiUsage")
abstract class RequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    open val objectMapper = jacksonObjectMapper()

    abstract val router: Router

    private val serializationHandlerChain by lazy { SerializationHandlerChain(serializationHandlers()) }
    private val deserializationHandlerChain by lazy { DeserializationHandlerChain(deserializationHandlers()) }

    @Suppress("UNCHECKED_CAST")
    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        input.headers = input.headers.mapKeys { it.key.toLowerCase() }
        log.debug("handling request with method '${input.httpMethod}' and path '${input.path}' - Accept:${input.acceptHeader()} Content-Type:${input.contentType()} $input")
        val routes = router.routes as List<RouterFunction<Any, Any>>
        val matchResults: List<RequestMatchResult> = routes.map { routerFunction: RouterFunction<Any, Any> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            log.debug("match result for route '$routerFunction' is '$matchResult'")
            if (matchResult.match) {
                val matchedAcceptType = routerFunction.requestPredicate.matchedAcceptType(input.acceptedMediaTypes())
                    ?: MediaType.parse(router.defaultContentType)
                if (!permissionHandlerSupplier()(input).hasAnyRequiredPermission(routerFunction.requestPredicate.requiredPermissions))
                    return createApiExceptionErrorResponse(
                        matchedAcceptType,
                        input,
                        ApiException("missing permissions", "MISSING_PERMISSIONS", 403)
                    )

                val handler: HandlerFunction<Any, Any> = routerFunction.handler
                return try {
                    val requestBody = deserializeRequest(handler, input)
                    val request =
                        Request(input, requestBody, routerFunction.requestPredicate.pathPattern)
                    val response = router.filter.then(handler as HandlerFunction<*, *>).invoke(request)
                    createResponse(matchedAcceptType, input, response)
                } catch (e: Exception) {
                    when (e) {
                        is ApiException -> createApiExceptionErrorResponse(matchedAcceptType, input, e)
                            .also { log.info("Caught api error while handling ${input.httpMethod} ${input.path} - $e") }
                        else -> createUnexpectedErrorResponse(matchedAcceptType, input, e)
                            .also { log.error("Caught exception handling ${input.httpMethod} ${input.path} - $e", e) }
                    }
                }
            }

            matchResult
        }
        return handleNonDirectMatch(MediaType.parse(router.defaultContentType), matchResults, input)
    }

    open fun serializationHandlers(): List<SerializationHandler> = listOf(
        JsonSerializationHandler(objectMapper)
    )

    open fun deserializationHandlers(): List<DeserializationHandler> = listOf(
        JsonDeserializationHandler(objectMapper)
    )

    open fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler =
        { NoOpPermissionHandler() }

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

    private fun handleNonDirectMatch(
        defaultContentType: MediaType,
        matchResults: List<RequestMatchResult>,
        input: APIGatewayProxyRequestEvent
    ): APIGatewayProxyResponseEvent {
        // no direct match
        val apiException =
            when {
                matchResults.any { it.matchPath && it.matchMethod && !it.matchContentType } ->
                    ApiException(
                        httpResponseStatus = 415,
                        message = "Unsupported Media Type",
                        code = "UNSUPPORTED_MEDIA_TYPE"
                    )
                matchResults.any { it.matchPath && it.matchMethod && !it.matchAcceptType } ->
                    ApiException(
                        httpResponseStatus = 406,
                        message = "Not Acceptable",
                        code = "NOT_ACCEPTABLE"
                    )
                matchResults.any { it.matchPath && !it.matchMethod } ->
                    ApiException(
                        httpResponseStatus = 405,
                        message = "Method Not Allowed",
                        code = "METHOD_NOT_ALLOWED"
                    )
                else -> ApiException(
                    httpResponseStatus = 404,
                    message = "Not found",
                    code = "NOT_FOUND"
                )
            }
        val contentType = input.acceptedMediaTypes().firstOrNull() ?: defaultContentType
        return createApiExceptionErrorResponse(contentType, input, apiException)
    }

    /**
     * Customize the format of an api error
     */
    open fun createErrorBody(error: ApiError): Any = error

    /**
     * Customize the format of an unprocessable entity error
     */
    open fun createUnprocessableEntityErrorBody(errors: List<UnprocessableEntityError>): Any = errors

    private fun createUnprocessableEntityErrorBody(error: UnprocessableEntityError): Any =
        createUnprocessableEntityErrorBody(listOf(error))

    open fun createApiExceptionErrorResponse(
        contentType: MediaType,
        input: APIGatewayProxyRequestEvent,
        ex: ApiException
    ): APIGatewayProxyResponseEvent =
        createErrorBody(ex.toApiError()).let {
            APIGatewayProxyResponseEvent()
                .withBody(
                    // in case of 406 we might find no serializer so fall back to the default
                    if (serializationHandlerChain.supports(contentType, it))
                        serializationHandlerChain.serialize(contentType, it)
                    else
                        serializationHandlerChain.serialize(MediaType.parse(router.defaultContentType), it)
                )
                .withStatusCode(ex.httpResponseStatus)
                .withHeaders(mapOf("Content-Type" to contentType.toString()))
        }

    open fun createUnexpectedErrorResponse(
        contentType: MediaType,
        input: APIGatewayProxyRequestEvent,
        ex: Exception
    ): APIGatewayProxyResponseEvent =
        when (ex) {
            is JsonParseException -> createResponse(
                contentType, input,
                ResponseEntity(
                    422, createUnprocessableEntityErrorBody(
                        UnprocessableEntityError(
                            message = "INVALID_ENTITY",
                            code = "ENTITY",
                            path = "",
                            details = mapOf(
                                "payload" to ex.requestPayloadAsString.orEmpty(),
                                "message" to ex.message.orEmpty()
                            )
                        )
                    )
                )
            )
            is InvalidDefinitionException -> createResponse(
                contentType, input,
                ResponseEntity(
                    422, createUnprocessableEntityErrorBody(
                        UnprocessableEntityError(
                            message = "INVALID_FIELD_FORMAT",
                            code = "FIELD",
                            path = ex.path.last().fieldName.orEmpty(),
                            details = mapOf(
                                "cause" to ex.cause?.message.orEmpty(),
                                "message" to ex.message.orEmpty()
                            )
                        )
                    )
                )
            )
            is InvalidFormatException ->
                createResponse(
                    contentType, input,
                    ResponseEntity(
                        422, createUnprocessableEntityErrorBody(
                            UnprocessableEntityError(
                                message = "INVALID_FIELD_FORMAT",
                                code = "FIELD",
                                path = ex.path.last().fieldName.orEmpty()
                            )
                        )
                    )
                )
            is MissingKotlinParameterException ->
                createResponse(
                    contentType, input,
                    ResponseEntity(
                        422, createUnprocessableEntityErrorBody(
                            UnprocessableEntityError(
                                message = "MISSING_REQUIRED_FIELDS",
                                code = "FIELD",
                                path = ex.parameter.name.orEmpty()
                            )
                        )
                    )
                )
            else -> createResponse(
                contentType, input,
                ResponseEntity(500, createErrorBody(ApiError(ex.message.orEmpty(), "INTERNAL_SERVER_ERROR")))
            )
        }

    open fun <T> createResponse(
        contentType: MediaType?,
        input: APIGatewayProxyRequestEvent,
        response: ResponseEntity<T>
    ): APIGatewayProxyResponseEvent {
        return when {
            // no-content response
            response.body == null -> APIGatewayProxyResponseEvent()
                .withStatusCode(response.statusCode)
                .withHeaders(response.headers)
            serializationHandlerChain.supports(contentType!!, response.body) ->
                APIGatewayProxyResponseEvent()
                    .withStatusCode(response.statusCode)
                    .withBody(serializationHandlerChain.serialize(contentType, response.body))
                    .withHeaders(response.headers + ("Content-Type" to contentType.toString()))
            else -> throw IllegalArgumentException("unsupported response ${response.body.let { (it as Any)::class.java }} and $contentType")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestHandler::class.java)
    }
}