/*
 * Copyright 2019 MOIA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

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
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

@Suppress("UnstableApiUsage")
abstract class RequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    open val objectMapper = jacksonObjectMapper()

    abstract val router: Router

    private val serializationHandlerChain by lazy { SerializationHandlerChain(serializationHandlers()) }
    private val deserializationHandlerChain by lazy { DeserializationHandlerChain(deserializationHandlers()) }

    override fun handleRequest(
        input: APIGatewayProxyRequestEvent,
        context: Context,
    ): APIGatewayProxyResponseEvent =
        input
            .apply { headers = headers.mapKeys { it.key.lowercase() } }
            .let { router.filter.then(this::handleRequest)(it) }

    @ExperimentalReflectionOnLambdas
    @Suppress("UNCHECKED_CAST")
    private fun handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        log.debug(
            "handling request with method '${input.httpMethod}' and path '${input.path}' - Accept:${input.acceptHeader()} Content-Type:${input.contentType()} $input",
        )
        val routes = router.routes as List<RouterFunction<Any, Any>>
        val matchResults: List<RequestMatchResult> =
            routes.map { routerFunction: RouterFunction<Any, Any> ->
                val matchResult = routerFunction.requestPredicate.match(input)
                log.debug("match result for route '$routerFunction' is '$matchResult'")
                if (matchResult.match) {
                    val matchedAcceptType =
                        routerFunction.requestPredicate.matchedAcceptType(input.acceptedMediaTypes())
                            ?: MediaType.parse(router.defaultContentType)

                    val handler: HandlerFunction<Any, Any> = routerFunction.handler

                    val response =
                        try {
                            if (missingPermissions(input, routerFunction)) {
                                throw ApiException("missing permissions", "MISSING_PERMISSIONS", 403)
                            } else {
                                val requestBody = deserializeRequest(handler, input)
                                val request = Request(input, requestBody, routerFunction.requestPredicate.pathPattern)
                                (handler as HandlerFunction<*, *>)(request)
                            }
                        } catch (e: Exception) {
                            exceptionToResponseEntity(e, input)
                        }
                    return createResponse(matchedAcceptType, response)
                }
                matchResult
            }
        return handleNonDirectMatch(MediaType.parse(router.defaultContentType), matchResults, input)
    }

    private fun exceptionToResponseEntity(
        e: Exception,
        input: APIGatewayProxyRequestEvent,
    ) = when (e) {
        is ApiException ->
            e.toResponseEntity(this::createErrorBody)
                .also { logApiException(e, input) }
        else ->
            exceptionToResponseEntity(e)
                .also { logUnknownException(e, input) }
    }

    private fun missingPermissions(
        input: APIGatewayProxyRequestEvent,
        routerFunction: RouterFunction<Any, Any>,
    ): Boolean {
        if (predicatePermissionHandlerSupplier() != null) {
            return !predicatePermissionHandlerSupplier()!!(input).hasAnyRequiredPermission(routerFunction.requestPredicate)
        }
        return !permissionHandlerSupplier()(input).hasAnyRequiredPermission(routerFunction.requestPredicate.requiredPermissions)
    }

    /**
     * Hook to be able to override the way ApiExceptions are logged.
     */
    open fun logApiException(
        e: ApiException,
        input: APIGatewayProxyRequestEvent,
    ) {
        log.info("Caught api error while handling ${input.httpMethod} ${input.path} - $e")
    }

    /**
     * Hook to be able to override the way non-ApiExceptions are logged.
     */
    open fun logUnknownException(
        e: Exception,
        input: APIGatewayProxyRequestEvent,
    ) {
        log.error("Caught exception handling ${input.httpMethod} ${input.path} - $e", e)
    }

    open fun serializationHandlers(): List<SerializationHandler> =
        listOf(
            JsonSerializationHandler(objectMapper),
            PlainTextSerializationHandler(),
        )

    open fun deserializationHandlers(): List<DeserializationHandler> =
        listOf(
            JsonDeserializationHandler(objectMapper),
        )

    open fun permissionHandlerSupplier(): (r: APIGatewayProxyRequestEvent) -> PermissionHandler = { NoOpPermissionHandler() }

    open fun predicatePermissionHandlerSupplier(): ((r: APIGatewayProxyRequestEvent) -> PredicatePermissionHandler)? = null

    @ExperimentalReflectionOnLambdas
    private fun deserializeRequest(
        handler: HandlerFunction<Any, Any>,
        input: APIGatewayProxyRequestEvent,
    ): Any? {
        val requestType =
            handler.reflect()?.parameters?.first()?.type?.arguments?.first()?.type
                ?: throw IllegalArgumentException(
                    "reflection failed, try using a real lambda instead of function references (Kotlin 1.6 bug?)",
                )
        return when {
            requestType.classifier as KClass<*> == Unit::class -> Unit
            input.body == null && requestType.isMarkedNullable -> null
            input.body == null -> throw ApiException("no request body present", "REQUEST_BODY_MISSING", 400)
            input.body is String && requestType.classifier as KClass<*> == String::class -> input.body
            else -> deserializationHandlerChain.deserialize(input, requestType)
        }
    }

    private fun handleNonDirectMatch(
        defaultContentType: MediaType,
        matchResults: List<RequestMatchResult>,
        input: APIGatewayProxyRequestEvent,
    ): APIGatewayProxyResponseEvent {
        // no direct match
        val apiException =
            when {
                matchResults.any { it.matchPath && it.matchMethod && !it.matchContentType } ->
                    ApiException(
                        httpResponseStatus = 415,
                        message = "Unsupported Media Type",
                        code = "UNSUPPORTED_MEDIA_TYPE",
                    )
                matchResults.any { it.matchPath && it.matchMethod && !it.matchAcceptType } ->
                    ApiException(
                        httpResponseStatus = 406,
                        message = "Not Acceptable",
                        code = "NOT_ACCEPTABLE",
                    )
                matchResults.any { it.matchPath && !it.matchMethod } ->
                    ApiException(
                        httpResponseStatus = 405,
                        message = "Method Not Allowed",
                        code = "METHOD_NOT_ALLOWED",
                    )
                else ->
                    ApiException(
                        httpResponseStatus = 404,
                        message = "Not found",
                        code = "NOT_FOUND",
                    )
            }
        return createResponse(
            contentType = input.acceptedMediaTypes().firstOrNull() ?: defaultContentType,
            response = apiException.toResponseEntity(this::createErrorBody),
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

    /**
     * Hook to customize the way non-ApiExceptions are converted to ResponseEntity.
     *
     * Some common exceptions are already handled in the default implementation.
     */
    open fun exceptionToResponseEntity(ex: Exception) =
        when (ex) {
            is JsonParseException ->
                ResponseEntity(
                    422,
                    createUnprocessableEntityErrorBody(
                        UnprocessableEntityError(
                            message = "INVALID_ENTITY",
                            code = "ENTITY",
                            path = "",
                            details =
                                mapOf(
                                    "payload" to ex.requestPayloadAsString.orEmpty(),
                                    "message" to ex.message.orEmpty(),
                                ),
                        ),
                    ),
                )
            is InvalidDefinitionException ->
                ResponseEntity(
                    422,
                    createUnprocessableEntityErrorBody(
                        UnprocessableEntityError(
                            message = "INVALID_FIELD_FORMAT",
                            code = "FIELD",
                            path = ex.path.last().fieldName.orEmpty(),
                            details =
                                mapOf(
                                    "cause" to ex.cause?.message.orEmpty(),
                                    "message" to ex.message.orEmpty(),
                                ),
                        ),
                    ),
                )
            is InvalidFormatException ->
                ResponseEntity(
                    422,
                    createUnprocessableEntityErrorBody(
                        UnprocessableEntityError(
                            message = "INVALID_FIELD_FORMAT",
                            code = "FIELD",
                            path = ex.path.last().fieldName.orEmpty(),
                        ),
                    ),
                )
            is MissingKotlinParameterException ->
                ResponseEntity(
                    422,
                    createUnprocessableEntityErrorBody(
                        UnprocessableEntityError(
                            message = "MISSING_REQUIRED_FIELDS",
                            code = "FIELD",
                            path = ex.parameter.name.orEmpty(),
                        ),
                    ),
                )
            else -> ResponseEntity(500, createErrorBody(ApiError(ex.message.orEmpty(), "INTERNAL_SERVER_ERROR")))
        }

    open fun <T> createResponse(
        contentType: MediaType,
        response: ResponseEntity<T>,
    ): APIGatewayProxyResponseEvent =
        when (response.body != null && serializationHandlerChain.supports(contentType, response.body)) {
            true -> contentType
            false -> MediaType.parse(router.defaultContentType)
        }.let { finalContentType ->
            APIGatewayProxyResponseEvent()
                .withStatusCode(response.statusCode)
                .withHeaders(response.headers.toMutableMap().apply { put("Content-Type", finalContentType.toString()) })
                .withBody(
                    response.body?.let {
                        serializationHandlerChain.serialize(finalContentType, it as Any)
                    },
                )
        }

    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestHandler::class.java)
    }
}
