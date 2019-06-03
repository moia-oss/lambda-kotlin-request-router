package io.moia.router

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

    fun toApiError() =
        ApiError(super.message!!, code, details)

    inline fun toResponseEntity(mapper: (error: ApiError) -> Any = {}) =
        ResponseEntity(httpResponseStatus, mapper(toApiError()))
}

data class ApiError(
    val message: String,
    val code: String,
    val details: Map<String, Any> = emptyMap()
)

data class UnprocessableEntityError(
    val message: String,
    val code: String,
    val path: String,
    val details: Map<String, Any> = emptyMap()
)