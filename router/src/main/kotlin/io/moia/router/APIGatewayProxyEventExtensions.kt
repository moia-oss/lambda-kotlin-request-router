package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import java.net.URI
import java.util.Base64

fun APIGatewayProxyRequestEvent.acceptHeader() = getHeaderCaseInsensitive("accept")
fun APIGatewayProxyRequestEvent.contentType() = getHeaderCaseInsensitive("content-type")

fun APIGatewayProxyRequestEvent.getHeaderCaseInsensitive(httpHeader: String): String? =
    getCaseInsensitive(httpHeader, headers)

fun APIGatewayProxyResponseEvent.getHeaderCaseInsensitive(httpHeader: String): String? =
    getCaseInsensitive(httpHeader, headers)

fun GET() = APIGatewayProxyRequestEvent().withHttpMethod("get").withHeaders(mutableMapOf())
fun GET(path: String) = GET().withPath(path)
fun POST() = APIGatewayProxyRequestEvent().withHttpMethod("post").withHeaders(mutableMapOf())
fun POST(path: String) = POST().withPath(path)
fun PUT() = APIGatewayProxyRequestEvent().withHttpMethod("put").withHeaders(mutableMapOf())
fun PUT(path: String) = PUT().withPath(path)
fun PATCH() = APIGatewayProxyRequestEvent().withHttpMethod("patch").withHeaders(mutableMapOf())
fun PATCH(path: String) = PATCH().withPath(path)
fun DELETE() = APIGatewayProxyRequestEvent().withHttpMethod("delete").withHeaders(mutableMapOf())
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
    val portPart = getHeaderCaseInsensitive("x-forwarded-port")
        ?.let {
            when {
                proto == "https" && it == "443" -> null
                proto == "http" && it == "80" -> null
                else -> ":$it"
            }
        } ?: ""
    return URI("$proto://$host$portPart/${path.removePrefix("/")}")
}

fun APIGatewayProxyRequestEvent.withHeader(name: String, value: String) =
    this.also { if (headers == null) headers = mutableMapOf() }.also { headers[name] = value }

fun APIGatewayProxyResponseEvent.withHeader(name: String, value: String) =
    this.also { if (headers == null) headers = mutableMapOf() }.also { headers[name] = value }

fun APIGatewayProxyResponseEvent.withLocationHeader(request: APIGatewayProxyRequestEvent, path: String) =
    this.also { if (headers == null) headers = mutableMapOf() }.also { headers["location"] = request.location(path).toString() }

fun APIGatewayProxyResponseEvent.location() = getHeaderCaseInsensitive("location")

private fun getCaseInsensitive(key: String, map: Map<String, String>?): String? =
    map?.entries
        ?.firstOrNull { key.equals(it.key, ignoreCase = true) }
        ?.value

fun APIGatewayProxyResponseEvent.bodyAsBytes() = Base64.getDecoder().decode(body)
