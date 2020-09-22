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
import com.google.common.net.MediaType
import isCompatibleWith

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
            matchAcceptType = acceptMatches(request.acceptedMediaTypes()),
            matchContentType = contentTypeMatches(request.contentType())
        )

    private fun pathMatches(request: APIGatewayProxyRequestEvent) =
        request.path?.let { UriTemplate.from(pathPattern).matches(it) } ?: false
    private fun methodMatches(request: APIGatewayProxyRequestEvent) = method.equals(request.httpMethod, true)

    /**
     * Find the media type that is compatible with the one the client requested out of the ones that the the handler can produce
     * Talking into account that an accept header can contain multiple media types (e.g. application/xhtml+xml, application/json)
     */
    fun matchedAcceptType(acceptedMediaTypes: List<MediaType>) =
        produces
            .map { MediaType.parse(it) }
            .firstOrNull { acceptedMediaTypes.any { acceptedType -> it.isCompatibleWith(acceptedType) } }

    private fun acceptMatches(acceptedMediaTypes: List<MediaType>) =
        matchedAcceptType(acceptedMediaTypes) != null

    private fun contentTypeMatches(contentType: String?) =
        when {
            consumes.isEmpty() -> true
            contentType == null -> false
            else -> consumes.any { MediaType.parse(contentType).isCompatibleWith(MediaType.parse(it)) }
        }
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
