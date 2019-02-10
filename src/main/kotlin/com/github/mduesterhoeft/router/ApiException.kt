package com.github.mduesterhoeft.router

import java.lang.RuntimeException

class ApiException(
    val statusCode: Int = 400,
    message: String,
    val errorCode: Int,
    val details: Map<String, Any> = emptyMap()
) : RuntimeException(message) {

    override fun toString(): String {
        return "ApiException(statusCode=$statusCode, errorCode=$errorCode, details=$details)"
    }
}