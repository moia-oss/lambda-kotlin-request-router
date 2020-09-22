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
