package com.github.mduesterhoeft.router.sample

import com.github.mduesterhoeft.router.ApiJsonResponse
import com.github.mduesterhoeft.router.RequestHandler
import com.github.mduesterhoeft.router.Router.Companion.router

class MyRequestHandler : RequestHandler {
    private val controller = SomeController()

    override val router = router {
        GET("/some", controller::get)
        POST("/some") {
            ApiJsonResponse(
                statusCode = 200,
                body = """{"hello": "world", "request":"${it.body}"}"""
            )
        }
    }
}