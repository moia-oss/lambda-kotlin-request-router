package com.github.mduesterhoeft.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import java.util.Base64

fun APIGatewayProxyRequestEvent.acceptHeader() = getHeaderCaseInsensitive("accept")
fun APIGatewayProxyRequestEvent.contentType() = getHeaderCaseInsensitive("content-type")

fun APIGatewayProxyRequestEvent.getHeaderCaseInsensitive(httpHeader: String): String? =
    getCaseInsensitive(httpHeader, headers)

fun APIGatewayProxyResponseEvent.getHeaderCaseInsensitive(httpHeader: String): String? =
    getCaseInsensitive(httpHeader, headers)

private fun getCaseInsensitive(key: String, map: Map<String, String>): String? =
    map.entries
        .firstOrNull { it.key.toLowerCase() == key.toLowerCase() }
        ?.value

fun APIGatewayProxyResponseEvent.bodyAsBytes() = Base64.getDecoder().decode(body)
