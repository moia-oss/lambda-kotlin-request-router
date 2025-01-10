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

import java.net.URLDecoder
import java.util.regex.Pattern

class UriTemplate private constructor(
    private val template: String,
) {
    private val templateRegex: Regex
    private val matches: Sequence<MatchResult>
    private val parameterNames: List<String>

    init {
        if (INVALID_GREEDY_PATH_VARIABLE_REGEX.matches(template)) {
            throw IllegalArgumentException("Greedy path variables (e.g. '{proxy+}' are only allowed at the end of the template")
        }
        matches = PATH_VARIABLE_REGEX.findAll(template)
        parameterNames = matches.map { it.groupValues[1] }.toList()
        templateRegex =
            template
                .replace(
                    PATH_VARIABLE_REGEX,
                    { notMatched -> Pattern.quote(notMatched) },
                    { matched ->
                        // check for greedy path variables, e.g. '{proxy+}'
                        if (matched.groupValues[1].endsWith("+")) {
                            return@replace "(.+)"
                        }
                        if (matched.groupValues[2].isBlank()) "([^/]+)" else "(${matched.groupValues[2]})"
                    },
                ).toRegex()
    }

    companion object {
        private val PATH_VARIABLE_REGEX = "\\{([^}]+?)(?::([^}]+))?}".toRegex()
        private val INVALID_GREEDY_PATH_VARIABLE_REGEX = ".*\\{([^}]+?)(?::([^}]+))?\\+}.+".toRegex()

        // Removes query params
        fun from(template: String) = UriTemplate(template.split('?')[0].trimSlashes())

        fun String.trimSlashes() = "^(/)?(.*?)(/)?$".toRegex().replace(this) { result -> result.groupValues[2] }
    }

    fun matches(uri: String): Boolean = templateRegex.matches(uri.trimSlashes())

    fun extract(uri: String): Map<String, String> = parameterNames.zip(templateRegex.findParameterValues(uri.trimSlashes())).toMap()

    private fun Regex.findParameterValues(uri: String): List<String> =
        findAll(uri)
            .first()
            .groupValues
            .drop(1)
            .map { URLDecoder.decode(it, "UTF-8") }

    private fun String.replace(
        regex: Regex,
        notMatched: (String) -> String,
        matched: (MatchResult) -> String,
    ): String {
        val matches = regex.findAll(this)
        val builder = StringBuilder()
        var position = 0
        for (matchResult in matches) {
            val before = substring(position, matchResult.range.start)
            if (before.isNotEmpty()) builder.append(notMatched(before))
            builder.append(matched(matchResult))
            position = matchResult.range.endInclusive + 1
        }
        val after = substring(position, length)
        if (after.isNotEmpty()) builder.append(notMatched(after))
        return builder.toString()
    }

    override fun toString(): String = template
}
