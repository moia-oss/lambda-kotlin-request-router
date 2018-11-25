package com.md.api
    data class RequestContext(var accountId: String? = null,
                              var resourceId: String? = null,
                              var stage: String? = null,
                              var requestId: String? = null,
                              var identity: MutableMap<String, String>? = null,
                              var authorizer: Authorizer? = null,
                              var resourcePath: String? = null,
                              var httpMethod: String? = null,
                              var apiId: String? = null)
    data class Authorizer(var principalId: String? = null, var claims: MutableMap<String, String>? = null)
    data class ApiRequest(var path: String? = null,
                          var source: String? = null,
                          var headers: MutableMap<String, String>? = null,
                          var pathParameters: MutableMap<String, String>? = null,
                          var requestContext: RequestContext? = null,
                          var resource: String? = null,
                          var httpMethod: String? = null,
                          var body: String? = null,
                          var isBase64Encoded: String? = null,
                          var queryStringParameters: MutableMap<String, String>? = null,
                          var stageVariables: MutableMap<String, String>? = null
    ) {


        private fun getHeaderCaseInsensitive(httpHeader: String): String? {
            return headers?.get(httpHeader) ?: headers?.get(httpHeader.toLowerCase())
        }

    }