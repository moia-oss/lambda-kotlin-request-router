package com.md.api

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.md.api.Router.Companion.router
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

interface RequestHandler: RequestHandler<ApiRequest, ApiResponse> {

    override fun handleRequest(input: ApiRequest, context: Context): ApiResponse? {
        log.info("handling request with method '${input.httpMethod}' and path '${input.path}'")
        val matchResults = mutableListOf<MatchResult>()
        router.routes.forEach {
            val matchResult = it.requestPredicate.match(input)
            log.info("match result for route '$it' is '$matchResult'")
            if (matchResult.match)
                return it.handler(input)
            else
                matchResults += matchResult
        }
        //no direct match
        if (matchResults.any { it.matchPath && it.matchMethod && !it.matchContentType }) {
            return ApiJsonResponse(statusCode = 415, body = """{ "error": "Unsupported Media Type" }""".trimMargin())
        }
        if (matchResults.any { it.matchPath && it.matchMethod && !it.matchAcceptType }) {
            return ApiJsonResponse(statusCode = 406, body = """{ "error": "Not Acceptable" }""".trimMargin())
        }
        if (matchResults.any { it.matchPath && !it.matchMethod }) {
            return ApiJsonResponse(statusCode = 405, body = """{ "error": "Method Not Allowed" }""".trimMargin())
        }
        return ApiJsonResponse(statusCode = 404, body = """{ "error": "Not found" }""")
    }

    val router: Router

    companion object {
        val log: Logger = LogManager.getLogger(RequestHandler::class.java)
    }
}