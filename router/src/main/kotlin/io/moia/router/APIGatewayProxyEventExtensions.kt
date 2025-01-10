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

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.google.common.net.MediaType
import java.net.URI
import java.util.Base64

/** Data class that represents an HTTP header */
data class Header(
    val name: String,
    val value: String,
)

fun APIGatewayProxyRequestEvent.acceptHeader() = getHeaderCaseInsensitive("accept")

fun APIGatewayProxyRequestEvent.acceptedMediaTypes() =
    acceptHeader()
        ?.split(",")
        ?.map { it.trim() }
        ?.mapNotNull { parseMediaTypeSafe(it) }
        .orEmpty()

fun APIGatewayProxyRequestEvent.contentType() = getHeaderCaseInsensitive("content-type")

fun APIGatewayProxyRequestEvent.getHeaderCaseInsensitive(httpHeader: String): String? = getCaseInsensitive(httpHeader, headers)

fun APIGatewayProxyResponseEvent.getHeaderCaseInsensitive(httpHeader: String): String? = getCaseInsensitive(httpHeader, headers)

@Suppress("FunctionName")
fun GET() = APIGatewayProxyRequestEvent().withHttpMethod("get").withHeaders(mutableMapOf())

@Suppress("FunctionName")
fun GET(path: String) = GET().withPath(path)

@Suppress("FunctionName")
fun POST() = APIGatewayProxyRequestEvent().withHttpMethod("post").withHeaders(mutableMapOf())

@Suppress("FunctionName")
fun POST(path: String) = POST().withPath(path)

@Suppress("FunctionName")
fun PUT() = APIGatewayProxyRequestEvent().withHttpMethod("put").withHeaders(mutableMapOf())

@Suppress("FunctionName")
fun PUT(path: String) = PUT().withPath(path)

@Suppress("FunctionName")
fun PATCH() = APIGatewayProxyRequestEvent().withHttpMethod("patch").withHeaders(mutableMapOf())

@Suppress("FunctionName")
fun PATCH(path: String) = PATCH().withPath(path)

@Suppress("FunctionName")
fun DELETE() = APIGatewayProxyRequestEvent().withHttpMethod("delete").withHeaders(mutableMapOf())

@Suppress("FunctionName")
fun DELETE(path: String) = DELETE().withPath(path)

/**
 * Get a URI that can be used as location header for responses.
 * The host is taken from the Host header.
 * The protocol is taken from the x-forwarded-proto.
 * The port is taken from the x-forwarded-port header. Standard ports are omitted.
 */
fun APIGatewayProxyRequestEvent.location(path: String): URI {
    val host = getHeaderCaseInsensitive("host") ?: "localhost"
    val proto = getHeaderCaseInsensitive("x-forwarded-proto") ?: "http"
    val portPart =
        getHeaderCaseInsensitive("x-forwarded-port")
            ?.let {
                when {
                    proto == "https" && it == "443" -> null
                    proto == "http" && it == "80" -> null
                    else -> ":$it"
                }
            } ?: ""
    return URI("$proto://$host$portPart/${path.removePrefix("/")}")
}

fun APIGatewayProxyRequestEvent.withHeader(
    name: String,
    value: String,
) = this.also { if (headers == null) headers = mutableMapOf() }.also { headers[name] = value }

fun APIGatewayProxyRequestEvent.withHeader(header: Header) = this.withHeader(header.name, header.value)

fun APIGatewayProxyRequestEvent.withAcceptHeader(accept: String) = this.withHeader("accept", accept)

fun APIGatewayProxyRequestEvent.withContentTypeHeader(contentType: String) = this.withHeader("content-type", contentType)

fun APIGatewayProxyResponseEvent.withHeader(
    name: String,
    value: String,
) = this.also { if (headers == null) headers = mutableMapOf() }.also { headers[name] = value }

fun APIGatewayProxyResponseEvent.withHeader(header: Header) = this.withHeader(header.name, header.value)

fun APIGatewayProxyResponseEvent.withLocationHeader(
    request: APIGatewayProxyRequestEvent,
    path: String,
) = this.also { if (headers == null) headers = mutableMapOf() }.also { headers["location"] = request.location(path).toString() }

fun APIGatewayProxyResponseEvent.location() = getHeaderCaseInsensitive("location")

private fun getCaseInsensitive(
    key: String,
    map: Map<String, String>?,
): String? =
    map
        ?.entries
        ?.firstOrNull { key.equals(it.key, ignoreCase = true) }
        ?.value

fun APIGatewayProxyResponseEvent.bodyAsBytes() = Base64.getDecoder().decode(body)

private fun parseMediaTypeSafe(input: String): MediaType? =
    try {
        MediaType.parse(input)
    } catch (e: IllegalArgumentException) {
        null
    }
