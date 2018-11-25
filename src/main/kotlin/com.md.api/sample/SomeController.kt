package com.md.api.sample

import com.md.api.ApiJsonResponse
import com.md.api.ApiRequest

class SomeController {

    fun get(request: ApiRequest) =
        ApiJsonResponse(statusCode = 200, body = """{"hello": "world", "request":"${request.body}"}""")
}