package io.moia.router

import java.lang.IllegalArgumentException
import java.net.URLDecoder
import java.util.regex.Pattern

class UriTemplate private constructor(private val template: String) {
    private val templateRegex: Regex
    private val matches: Sequence<MatchResult>
    private val parameterNames: List<String>

    init {
        matches = PATH_VARIABLE_REGEX.findAll(template)
        parameterNames = matches.map { it.groupValues[1] }.toList()
        templateRegex = template.replace(
            PATH_VARIABLE_REGEX,
            { notMatched -> Pattern.quote(notMatched) },
            { matched ->
                // check for greedy path variables, e.g. '{proxy+}'
                if (matched.groupValues[1].endsWith("+")) {
                    if (matched.next() != null) throw IllegalArgumentException("Greedy path variables (e.g. '{proxy+}' are only allowed at the end of the template")
                    return@replace "(.+)"
                }
                if (matched.groupValues[2].isBlank()) "([^/]+)" else "(${matched.groupValues[2]})"
            }
        ).toRegex()
    }

    companion object {
        private val PATH_VARIABLE_REGEX = "\\{([^}]+?)(?::([^}]+))?}".toRegex()

        // Removes query params
        fun from(template: String) = UriTemplate(template.split('?')[0].trimSlashes())

        fun String.trimSlashes() = "^(/)?(.*?)(/)?$".toRegex().replace(this) { result -> result.groupValues[2] }
    }

    fun matches(uri: String): Boolean = templateRegex.matches(uri.trimSlashes())

    fun extract(uri: String): Map<String, String> = parameterNames.zip(templateRegex.findParameterValues(uri.trimSlashes())).toMap()

    private fun Regex.findParameterValues(uri: String): List<String> =
        findAll(uri).first().groupValues.drop(1).map { URLDecoder.decode(it, "UTF-8") }

    private fun String.replace(regex: Regex, notMatched: (String) -> String, matched: (MatchResult) -> String): String {
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