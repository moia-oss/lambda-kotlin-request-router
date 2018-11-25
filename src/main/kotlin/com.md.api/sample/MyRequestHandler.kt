package com.md.api.sample

import com.md.api.ApiJsonResponse
import com.md.api.RequestHandler
import com.md.api.Router.Companion.router

class MyRequestHandler: RequestHandler {
    private val controller = SomeController()

    override val router= router {
        GET("/some", controller::get)
        POST("/some") {
            ApiJsonResponse(
                statusCode = 200,
                body = """{"hello": "world", "request":"${it.body}"}"""
            )
        }
    }
}