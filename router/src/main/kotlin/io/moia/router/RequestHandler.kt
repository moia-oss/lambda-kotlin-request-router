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
        log.debug("handling request with method '${input.httpMethod}' and path '${input.path}' - Accept:${input.acceptHeader()} Content-Type:${input.contentType()} $input")
        val routes = router.routes as List<RouterFunction<Any, Any>>
        val matchResults: List<RequestMatchResult> = routes.map { routerFunction: RouterFunction<Any, Any> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            log.debug("match result for route '$routerFunction' is '$matchResult'")
            if (matchResult.match) {

                val matchedAcceptType = routerFunction.requestPredicate.matchedAcceptType(input.acceptedMediaTypes())
                    ?: MediaType.parse(router.defaultContentType)

                // Phase 1: Deserialization
                // TODO: find a way to also invoke the filter chain on failed deserialization
                val handler: HandlerFunction<Any, Any> = routerFunction.handler
                val requestBody = try {
                    deserializeRequest(handler, input)
                } catch (e: Exception) {
                    return createResponse(matchedAcceptType, exceptionToResponseEntity(e))
                }

                // Phase 2: Content Handling
                val request = Request(input, requestBody, routerFunction.requestPredicate.pathPattern)
                return createResponse(matchedAcceptType, router.filter.then {
                    try {
                        when {
                            missingPermissions(input, routerFunction) ->
                                ResponseEntity(403, ApiError("missing permissions", "MISSING_PERMISSIONS"))
                            else -> (handler as HandlerFunction<*, *>)(request)
                        }
                    } catch (e: Exception) {
                        exceptionToResponseEntity(e, input)
                    }
                }(request))
            }
            matchResult
        }
        return handleNonDirectMatch(MediaType.parse(router.defaultContentType), matchResults, input)
    }

    private fun exceptionToResponseEntity(e: Exception, input: APIGatewayProxyRequestEvent) =
        when (e) {
            is ApiException -> e.toResponseEntity(this::createErrorBody)
                .also { log.info("Caught api error while handling ${input.httpMethod} ${input.path} - $e") }
            else -> exceptionToResponseEntity(e)
                .also {
                    log.error("Caught exception handling ${input.httpMethod} ${input.path} - $e", e)
                }
        }

    private fun missingPermissions(input: APIGatewayProxyRequestEvent, routerFunction: RouterFunction<Any, Any>) =
        !permissionHandlerSupplier()(input).hasAnyRequiredPermission(routerFunction.requestPredicate.requiredPermissions)

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

    private fun handleNonDirectMatch(defaultContentType: MediaType, matchResults: List<RequestMatchResult>, input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
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
        return createResponse(
            contentType = input.acceptedMediaTypes().firstOrNull() ?: defaultContentType,
            response = apiException.toResponseEntity(this::createErrorBody)
        )
    }

    /**
     * Customize the format of an api error
     */
    open fun createErrorBody(error: ApiError): Any = error

    /**
     * Customize the format of an unprocessable entity error
     */
    open fun createUnprocessableEntityErrorBody(errors: List<UnprocessableEntityError>): Any = errors

    private fun createUnprocessableEntityErrorBody(error: UnprocessableEntityError): Any = createUnprocessableEntityErrorBody(listOf(error))

    open fun exceptionToResponseEntity(ex: Exception) =
        when (ex) {
            is JsonParseException -> ResponseEntity(
                422,
                createUnprocessableEntityErrorBody(
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
            is InvalidDefinitionException -> ResponseEntity(
                422,
                createUnprocessableEntityErrorBody(
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
            is InvalidFormatException -> ResponseEntity(
                422,
                createUnprocessableEntityErrorBody(
                    UnprocessableEntityError(
                        message = "INVALID_FIELD_FORMAT",
                        code = "FIELD",
                        path = ex.path.last().fieldName.orEmpty()
                    )
                )
            )
            is MissingKotlinParameterException -> ResponseEntity(
                422,
                createUnprocessableEntityErrorBody(
                    UnprocessableEntityError(
                        message = "MISSING_REQUIRED_FIELDS",
                        code = "FIELD",
                        path = ex.parameter.name.orEmpty()
                    )
                )
            )
            else -> ResponseEntity(500, createErrorBody(ApiError(ex.message.orEmpty(), "INTERNAL_SERVER_ERROR")))
        }

    open fun <T> createResponse(contentType: MediaType, response: ResponseEntity<T>): APIGatewayProxyResponseEvent =
        when (response.body != null && serializationHandlerChain.supports(contentType, response.body)) {
            true -> contentType
            false -> MediaType.parse(router.defaultContentType)
        }.let { finalContentType ->
            APIGatewayProxyResponseEvent()
                .withStatusCode(response.statusCode)
                .withHeaders(response.headers + ("Content-Type" to finalContentType.toString()))
                .withBody(response.body?.let {
                    serializationHandlerChain.serialize(finalContentType, it as Any)
                })
        }

    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestHandler::class.java)
    }
}