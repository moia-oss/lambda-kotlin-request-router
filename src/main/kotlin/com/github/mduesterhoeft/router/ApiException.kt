package com.github.mduesterhoeft.router

import java.lang.RuntimeException

class ApiException(
    val statusCode: Int = 400,
    message: String,
    val code: String,
    val details: Map<String, Any> = emptyMap()
) : RuntimeException(message) {

    override fun toString(): String {
        return "ApiException(statusCode=$statusCode, code=$code, details=$details, message=$message)"
    }
}