package com.github.mduesterhoeft.router

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.mduesterhoeft.router.Router.Companion.router
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

abstract class RequestHandler : RequestHandler<ApiRequest, ApiResponse> {

    val objectMapper: ObjectMapper = jacksonObjectMapper()

    override fun handleRequest(input: ApiRequest, context: Context): ApiResponse? {
        log.info("handling request with method '${input.httpMethod}' and path '${input.path}'")
        val matchResults: List<MatchResult> = router.routes.map {
            val matchResult = it.requestPredicate.match(input)
            log.info("match result for route '$it' is '$matchResult'")
            if (matchResult.match)
                return createResponse(input, it.handler(input))

            matchResult
        }
        return handleNonDirectMatch(matchResults, input)
    }

    private fun handleNonDirectMatch(matchResults: List<MatchResult>, input: ApiRequest): ApiResponse {
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

    open fun createErrorResponse(input: ApiRequest, ex: ApiException): ApiResponse =
        ApiJsonResponse(
            statusCode = ex.statusCode,
            headers = mapOf("Content-Type" to "application/json"),
            body = objectMapper.writeValueAsString(ex)
        )

    open fun <T> createResponse(input: ApiRequest, response: ResponseEntity<T>) = ApiJsonResponse(
        statusCode = 200,
        headers = mapOf("Content-Type" to "application/json"),
        body = response.body?.let { objectMapper.writeValueAsString(it) }
    )

    companion object {
        val log: Logger = LogManager.getLogger(RequestHandler::class.java)
    }
}