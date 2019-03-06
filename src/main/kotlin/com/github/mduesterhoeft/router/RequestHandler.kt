package com.github.mduesterhoeft.router

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mduesterhoeft.router.ProtoBufUtils.toJsonWithoutWrappers
import com.google.common.net.MediaType
import com.google.protobuf.GeneratedMessageV3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.logging.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.jvm.reflect

abstract class RequestHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    open val objectMapper = jacksonObjectMapper()

    @Suppress("UNCHECKED_CAST")
    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent? {
        log.info("handling request with method '${input.httpMethod}' and path '${input.path}' - Accept:${input.acceptHeader()} Content-Type:${input.contentType()} $input")
        val routes = router.routes as List<RouterFunction<Any, Any>>
        val matchResults: List<MatchResult> = routes.map { routerFunction: RouterFunction<Any, Any> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            log.info("match result for route '$routerFunction' is '$matchResult'")
            if (matchResult.match) {
                val handler: HandlerFunction<Any, Any> = routerFunction.handler
                val request = deserializeRequest(handler, input)
                val response = handler(Request(input, request))
                return createResponse(input, response)
            }

            matchResult
        }
        return handleNonDirectMatch(matchResults, input)
    }

    private fun deserializeRequest(
        handler: HandlerFunction<Any, Any>,
        input: APIGatewayProxyRequestEvent
    ): Any {
        val requestType = handler.reflect()!!.parameters.first().type.arguments.first().type?.classifier as KClass<*>
        return when (requestType) {
            Unit::class -> Unit
            String::class -> input.body!!
            else -> objectMapper.readValue(input.body, requestType.java)
        }
    }

    private fun handleNonDirectMatch(matchResults: List<MatchResult>, input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        // no direct match
        if (matchResults.any { it.matchPath && it.matchMethod && !it.matchContentType }) {
            return createErrorResponse(
                input, ApiException(
                    statusCode = 415,
                    message = "Unsupported Media Type",
                    errorCode = 1
                )
            )
        }
        if (matchResults.any { it.matchPath && it.matchMethod && !it.matchAcceptType }) {
            return createErrorResponse(
                input, ApiException(
                    statusCode = 406,
                    message = "Not Acceptable",
                    errorCode = 2
                )
            )
        }
        if (matchResults.any { it.matchPath && !it.matchMethod }) {
            return createErrorResponse(
                input, ApiException(
                    statusCode = 405,
                    message = "Method Not Allowed",
                    errorCode = 3
                )
            )
        }
        return createErrorResponse(
            input, ApiException(
                statusCode = 404,
                message = "Not found",
                errorCode = 4
            )
        )
    }

    abstract val router: Router

    open fun createErrorResponse(input: APIGatewayProxyRequestEvent, ex: ApiException): APIGatewayProxyResponseEvent =
        APIGatewayProxyResponseEvent()
            .withBody(objectMapper.writeValueAsString(mapOf(
                "message" to ex.message,
                "code" to ex.errorCode,
                "details" to ex.details
            )))
            .withStatusCode(ex.statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))

    open fun <T> createResponse(input: APIGatewayProxyRequestEvent, response: ResponseEntity<T>): APIGatewayProxyResponseEvent {
        val accept = MediaType.parse(input.acceptHeader())
        return when {
            response.body is Unit -> APIGatewayProxyResponseEvent()
                .withStatusCode(204)

            accept.`is`(MediaType.parse("application/x-protobuf")) -> APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(Base64.getEncoder().encodeToString((response.body as GeneratedMessageV3).toByteArray()))
                .withHeaders(mapOf("Content-Type" to "application/x-protobuf"))

            accept.`is`(MediaType.parse("application/json")) ->
                if (response.body is GeneratedMessageV3)
                    APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(toJsonWithoutWrappers(response.body))
                        .withHeaders(mapOf("Content-Type" to "application/json"))
                else
                    APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(response.body?.let { objectMapper.writeValueAsString(it) })
                        .withHeaders(mapOf("Content-Type" to "application/json"))
            else -> throw IllegalArgumentException("unsupported response $response")
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(RequestHandler::class.java)
    }
}