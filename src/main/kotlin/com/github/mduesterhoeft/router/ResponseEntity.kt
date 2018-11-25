package com.github.mduesterhoeft.router

data class ResponseEntity<T>(
    val statusCode: Int,
    val body: T? = null,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        fun <T> ok(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(200, body, headers
        )
    }
}