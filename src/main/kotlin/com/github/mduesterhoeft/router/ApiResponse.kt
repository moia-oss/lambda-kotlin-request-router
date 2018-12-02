package com.github.mduesterhoeft.router

interface ApiResponse {
    val statusCode: Int
    val headers: Map<String, String>
    val body: Any?
    val isBase64Encoded: Boolean
}

class ApiJsonResponse(
    override val statusCode: Int,
    override val headers: Map<String, String> = mapOf(),
    override val body: String?,
    override val isBase64Encoded: Boolean = false
) : ApiResponse

class ApiProtoResponse(override val statusCode: Int,
                       override val headers: Map<String, String> = mapOf(),
                       override val body: ByteArray,
                       override val isBase64Encoded: Boolean = true) : ApiResponse


