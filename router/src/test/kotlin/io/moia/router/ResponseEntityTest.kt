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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class ResponseEntityTest {
    private val body = "body"
    private val headers =
        mapOf(
            "content-type" to "text/plain",
        )

    @Test
    fun `should process ok response`() {
        val response = ResponseEntity.ok(body, headers)

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.headers).isNotEmpty()
        assertThat(response.body).isNotNull()
    }

    @Test
    fun `should process accepted response`() {
        val response = ResponseEntity.accepted(body, headers)

        assertThat(response.statusCode).isEqualTo(202)
        assertThat(response.headers).isNotEmpty()
        assertThat(response.body).isNotNull()
    }

    @Test
    fun `should process no content response`() {
        val response = ResponseEntity.noContent(headers)

        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.headers).isNotEmpty()
        assertThat(response.body).isNull()
    }

    @Test
    fun `should process bad request response`() {
        val response = ResponseEntity.badRequest(body, headers)

        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.headers).isNotEmpty()
        assertThat(response.body).isNotNull()
    }

    @Test
    fun `should process not found response`() {
        val response = ResponseEntity.notFound(body, headers)

        assertThat(response.statusCode).isEqualTo(404)
        assertThat(response.headers).isNotEmpty()
        assertThat(response.body).isNotNull()
    }

    @Test
    fun `should process unprocessable entity response`() {
        val response = ResponseEntity.unprocessableEntity(body, headers)

        assertThat(response.statusCode).isEqualTo(422)
        assertThat(response.headers).isNotEmpty()
        assertThat(response.body).isNotNull()
    }
}
