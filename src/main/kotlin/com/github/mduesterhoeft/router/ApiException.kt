package com.github.mduesterhoeft.router

open class ApiException(
    message: String,
    val code: String,
    val httpResponseStatus: Int,
    val details: Map<String, Any> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    override fun toString(): String {
        return "ApiException(message='$message', code='$code', httpResponseStatus=$httpResponseStatus, details=$details, cause=$cause)"
    }
}