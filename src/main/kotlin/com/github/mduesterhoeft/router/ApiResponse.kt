package com.github.mduesterhoeft.router

abstract class ApiResponse {
    abstract val statusCode: Int
    abstract val headers: MutableMap<String, String>
    abstract val body: Any
    abstract val isBase64Encoded: Boolean
}

class ApiJsonResponse(
    override val statusCode: Int,
    override val headers: MutableMap<String, String> = mutableMapOf(),
    override val body: String,
    override val isBase64Encoded: Boolean = false
) : ApiResponse()

class ApiProtoResponse(
    override val statusCode: Int,
    override val headers: MutableMap<String, String> = mutableMapOf(),
    override val body: ByteArray,
    override val isBase64Encoded: Boolean = true
) : ApiResponse()
