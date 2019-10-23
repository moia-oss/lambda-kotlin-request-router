package io.moia.router

import java.net.URI

data class ResponseEntity<T>(
    val statusCode: Int,
    val body: T? = null,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        fun <T> ok(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(200, body, headers)

        fun <T> created(body: T? = null, location: URI? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(201, body, if (location == null) headers else headers + ("location" to location.toString()))

        fun <T> accepted(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(202, body, headers)

        fun noContent(headers: Map<String, String> = emptyMap()) =
            ResponseEntity<Unit>(204, null, headers)

        fun <T> badRequest(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(400, body, headers)

        fun <T> notFound(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(404, body, headers)

        fun <T> unprocessableEntity(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(422, body, headers)
    }
}