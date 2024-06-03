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

import com.google.common.net.MediaType
import isCompatibleWith
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class MediaTypeTest {
    @Test
    fun `should match`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("application/json"))).isTrue()
    }

    @Test
    fun `should match subtype wildcard`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("application/*"))).isTrue()
    }

    @Test
    fun `should not match subtype wildcard in different tpye`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("image/*"))).isFalse()
    }

    @Test
    fun `should match wildcard`() {
        then(MediaType.parse("application/json").isCompatibleWith(MediaType.parse("*/*"))).isTrue()
    }

    @Test
    fun `should match wildcard structured syntax suffix`() {
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/vnd.moia+json"))).isTrue()
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/vnd.moia.v1+json"))).isTrue()
    }

    @Test
    fun `should not match wildcard structured syntax suffix on non suffix type`() {
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/json"))).isFalse()
    }

    @Test
    fun `should not match wildcard structured syntax suffix on differnt suffix`() {
        then(MediaType.parse("application/*+json").isCompatibleWith(MediaType.parse("application/*+x-protobuf"))).isFalse()
    }
}
