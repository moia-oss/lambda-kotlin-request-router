package com.github.mduesterhoeft.router

import com.google.common.net.MediaType

data class RequestPredicate(
    val method: String,
    val pathPattern: String,
    var produces: Set<String> = setOf("application/json", "application/x-protobuf"),
    var consumes: Set<String> = setOf("application/json", "application/x-protobuf")
) {

    fun consuming(vararg mediaTypes: String) {
        consumes = mediaTypes.toSet()
    }
    fun producing(vararg mediaTypes: String) {
        produces = mediaTypes.toSet()
    }

    internal fun match(request: ApiRequest) =
        MatchResult(
            matchPath = pathMatches(request),
            matchMethod = methodMatches(request),
            matchAcceptType = contentTypeMatches(
                request.headers?.entries?.firstOrNull { it.key.toLowerCase() == "accept" }?.value,
                produces
            ),
            matchContentType = contentTypeMatches(
                request.headers?.entries?.firstOrNull { it.key.toLowerCase() == "content-type" }?.value,
                consumes
            )
        )

    private fun pathMatches(request: ApiRequest) = request.path == pathPattern
    private fun methodMatches(request: ApiRequest) = method.equals(request.httpMethod, true)
    private fun contentTypeMatches(contentType: String?, accepted: Set<String>) =
        if (accepted.isEmpty() && contentType == null) true
        else if (contentType == null) false
        else accepted.any { MediaType.parse(contentType).`is`(MediaType.parse(it)) }

    companion object
}

internal data class MatchResult(
    val matchPath: Boolean = false,
    val matchMethod: Boolean = false,
    val matchAcceptType: Boolean = false,
    val matchContentType: Boolean = false
) {
    val match
        get() = matchPath && matchMethod && matchAcceptType && matchContentType
}