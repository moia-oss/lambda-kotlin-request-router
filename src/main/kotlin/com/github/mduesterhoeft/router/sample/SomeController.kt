package com.github.mduesterhoeft.router.sample

import com.github.mduesterhoeft.router.ApiJsonResponse
import com.github.mduesterhoeft.router.ApiRequest

class SomeController {

    fun get(request: ApiRequest) =
        ApiJsonResponse(
            statusCode = 200,
            body = """{"hello": "world", "request":"${request.body}"}"""
        )
}