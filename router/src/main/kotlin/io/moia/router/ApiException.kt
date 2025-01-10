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

open class ApiException(
    message: String,
    val code: String,
    val httpResponseStatus: Int,
    val details: Map<String, Any> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    override fun toString(): String =
        "ApiException(message='$message', code='$code', httpResponseStatus=$httpResponseStatus, details=$details, cause=$cause)"

    fun toApiError() = ApiError(super.message!!, code, details)

    inline fun toResponseEntity(mapper: (error: ApiError) -> Any = {}) = ResponseEntity(httpResponseStatus, mapper(toApiError()))
}

data class ApiError(
    val message: String,
    val code: String,
    val details: Map<String, Any> = emptyMap(),
)

data class UnprocessableEntityError(
    val message: String,
    val code: String,
    val path: String,
    val details: Map<String, Any> = emptyMap(),
)
