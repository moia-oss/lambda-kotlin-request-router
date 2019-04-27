package io.moia.router

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.google.common.net.MediaType

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

    /**
     * Register required permissions for this route.
     * The RequestHandler checks if any of the given permissions are found on a request.
     */
    fun requiringPermissions(vararg permissions: String): RequestPredicate {
        requiredPermissions = permissions.toSet()
        return this
    }

    internal fun match(request: APIGatewayProxyRequestEvent) =
        RequestMatchResult(
            matchPath = pathMatches(request),
            matchMethod = methodMatches(request),
            matchAcceptType = acceptMatches(request.acceptHeader()),
            matchContentType = contentTypeMatches(request.contentType())
        )

    private fun pathMatches(request: APIGatewayProxyRequestEvent) =
        request.path?.let { UriTemplate.from(pathPattern).matches(it) } ?: false
    private fun methodMatches(request: APIGatewayProxyRequestEvent) = method.equals(request.httpMethod, true)

    /**
     * Find the media type that is compatible with the one the client requested out of the ones that the the handler can produce
     * Talking into account that an accept header can contain multiple media types (e.g. application/xhtml+xml, application/json)
     */
    fun matchedAcceptType(acceptType: String?) =
        if (produces.isEmpty() || acceptType == null) null
        else produces
            .map { MediaType.parse(it) }
            // find the first media type that can be produced that is compatible with the requested type
            .firstOrNull { acceptType.split(",")
                .map { p -> p.trim() }
                .any { m -> MediaType.parse(m).`is`(it) } }

    private fun acceptMatches(contentType: String?) =
        matchedAcceptType(contentType) != null

    private fun contentTypeMatches(contentType: String?) =
        if (consumes.isEmpty() && contentType == null) true
        else if (contentType == null) false
        else consumes.any { MediaType.parse(contentType).`is`(MediaType.parse(it)) }
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