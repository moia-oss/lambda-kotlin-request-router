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

        fun <T> created(body: T? = null, location: URI, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(201, body, headers + ("location" to location.toString()))

        fun noContent(headers: Map<String, String> = emptyMap()) =
            ResponseEntity<Unit>(204, null, headers)
    }
}