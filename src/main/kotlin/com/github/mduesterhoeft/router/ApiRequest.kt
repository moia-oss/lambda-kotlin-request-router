package com.github.mduesterhoeft.router
    data class RequestContext(
        var accountId: String? = null,
        var resourceId: String? = null,
        var stage: String? = null,
        var requestId: String? = null,
        var identity: MutableMap<String, String>? = null,
        var authorizer: Authorizer? = null,
        var resourcePath: String? = null,
        var httpMethod: String? = null,
        var apiId: String? = null
    )
    data class Authorizer(var principalId: String? = null, var claims: MutableMap<String, String>? = null)
    data class ApiRequest(
        var path: String? = null,
        var source: String? = null,
        var headers: MutableMap<String, String> = mutableMapOf(),
        var pathParameters: MutableMap<String, String>? = null,
        var requestContext: RequestContext? = null,
        var resource: String? = null,
        var httpMethod: String? = null,
        var body: String? = null,
        var isBase64Encoded: String? = null,
        var queryStringParameters: MutableMap<String, String>? = null,
        var stageVariables: MutableMap<String, String>? = null
    ) {

        val acceptHeader by lazy { getHeaderCaseInsensitive("Accept") }
        val contentType by lazy { getHeaderCaseInsensitive("Content-Type") }

        private fun getHeaderCaseInsensitive(httpHeader: String): String? {
            return headers.entries
                .firstOrNull { it.key.toLowerCase() == httpHeader.toLowerCase() }
                ?.value
        }
    }