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

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

class UriTemplateTest {

    @ParameterizedTest
    @MethodSource("matchTestParams")
    fun `match template`(uriTemplate: String, matchTemplate: String, expectedResult: Boolean) {
        then(UriTemplate.from(uriTemplate).matches(matchTemplate)).isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @MethodSource("extractTestParams")
    fun `extract template`(uriTemplate: String, extractTemplate: String, expectedResult: Map<String, String>) {
        then(UriTemplate.from(uriTemplate).extract(extractTemplate)).isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @MethodSource("notAllowedGreedyPathTemplates")
    fun `should throw exception for greedy path variables at the wrong place`(testedValue: String) {
        assertThrows<IllegalArgumentException>("Greedy path variables (e.g. '{proxy+}' are only allowed at the end of the template") {
            UriTemplate.from(testedValue)
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun matchTestParams() = listOf(
            Arguments.of("/some", "/some", true, "should match without parameter"),
            Arguments.of("/some", "/some-other", false, "should not match simple"),
            Arguments.of("/some/{id}", "/some/${UUID.randomUUID()}", true, "should match with parameter-1"),
            Arguments.of("/some/{id}/other", "/some/${UUID.randomUUID()}/other", true, "should match with parameter-2"),
            Arguments.of("/some/{id}", "/some-other/${UUID.randomUUID()}", false, "should not match with parameter-1"),
            Arguments.of(
                "/some/{id}/other",
                "/some/${UUID.randomUUID()}/other-test",
                false,
                "should not match with parameter-2"
            ),
            Arguments.of("/some?a=1", "/some", true, "should match with query parameter 1"),
            Arguments.of("/some?a=1&b=2", "/some", true, "should match with query parameter 2"),
            Arguments.of(
                "/some/{id}?a=1",
                "/some/${UUID.randomUUID()}",
                true,
                "should match with path parameter and query parameter 1"
            ),
            Arguments.of(
                "/some/{id}/other?a=1&b=2",
                "/some/${UUID.randomUUID()}/other",
                true,
                "should match with path parameter and query parameter 2"
            ),
            Arguments.of(
                "/some/{proxy+}",
                "/some/sub/sub/sub/path",
                true,
                "should handle greedy path variables successfully"
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun extractTestParams() = listOf(
            Arguments.of("/some", "/some", emptyMap<String, String>(), "should extract parameters-1"),
            Arguments.of(
                "/some/{first}/other/{second}",
                "/some/first-value/other/second-value",
                mapOf("first" to "first-value", "second" to "second-value"),
                "should extract parameters 2"
            )
        )

        @JvmStatic
        @Suppress("unused")
        fun notAllowedGreedyPathTemplates() = listOf(
            "/some/{proxy+}/and/{variable}/error",
            "/{proxy+}/some/and/{variable}/error",
            "/here/some/and/{proxy+}/{variable}",
            "/here/some/and/{proxy+}/error", // FIXME: it should throw exception
            "/here/some/and//good/good/{proxy+}/bad/bad/bad", // FIXME: it should throw exception
            "/{proxy+}/{id}",
            "/{proxy+}/whatever"
        )
    }
}