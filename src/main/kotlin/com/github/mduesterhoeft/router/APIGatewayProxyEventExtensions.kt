package com.github.mduesterhoeft.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import java.util.Base64

fun APIGatewayProxyRequestEvent.acceptHeader() = getHeaderCaseInsensitive("accept")
fun APIGatewayProxyRequestEvent.contentType() = getHeaderCaseInsensitive("content-type")

fun APIGatewayProxyRequestEvent.getHeaderCaseInsensitive(httpHeader: String): String? {
    return headers.entries
        .firstOrNull { it.key.toLowerCase() == httpHeader.toLowerCase() }
        ?.value
}

fun APIGatewayProxyResponseEvent.bodyAsBytes() = Base64.getDecoder().decode(body)
