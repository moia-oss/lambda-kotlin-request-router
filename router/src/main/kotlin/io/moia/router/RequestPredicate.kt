package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.apache.http.entity.ContentType

data class RequestPredicate(
    val method: String,
    val pathPattern: String,
    var produces: Set<String>,
    var consumes: Set<String>,
    var requiredPermissions: Set<String> = emptySet()
) {

    fun consuming(vararg mediaTypes: String): RequestPredicate {
        consumes = mediaTypes.toSet()
        return this
    }

    fun producing(vararg mediaTypes: String): RequestPredicate {
        produces = mediaTypes.toSet()
        return this
    }

    fun requiringPermissions(vararg permissions: String): RequestPredicate {
        requiredPermissions = permissions.toSet()
        return this
    }

    internal fun match(request: APIGatewayProxyRequestEvent) =
        RequestMatchResult(
            matchPath = pathMatches(request),
            matchMethod = methodMatches(request),
            matchAcceptType = contentTypeMatches(request.acceptHeader(), produces),
            matchContentType = contentTypeMatches(request.contentType(), consumes)
        )

    private fun pathMatches(request: APIGatewayProxyRequestEvent) = UriTemplate.from(
        pathPattern
    ).matches(request.path)
    private fun methodMatches(request: APIGatewayProxyRequestEvent) = method.equals(request.httpMethod, true)
    private fun contentTypeMatches(contentType: String?, accepted: Set<String>) =
        if (accepted.isEmpty() && contentType == null) true
        else if (contentType == null) false
        else accepted.any { ContentType.parse(contentType).mimeType == ContentType.parse(it).mimeType }

    companion object
}

internal data class RequestMatchResult(
    val matchPath: Boolean = false,
    val matchMethod: Boolean = false,
    val matchAcceptType: Boolean = false,
    val matchContentType: Boolean = false
) {
    val match
        get() = matchPath && matchMethod && matchAcceptType && matchContentType
}