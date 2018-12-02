package com.github.mduesterhoeft.router.sample

import com.github.mduesterhoeft.router.ApiJsonResponse
import com.github.mduesterhoeft.router.ApiRequest
import com.github.mduesterhoeft.router.RequestHandler
import com.github.mduesterhoeft.router.ResponseEntity
import com.github.mduesterhoeft.router.Router.Companion.router
import com.github.mduesterhoeft.router.sample.proto.SampleOuterClass

class MyRequestHandler : RequestHandler() {
    private val controller = SomeController()

    override val router = router {
        GET("/some", controller::get)
        POST("/some") {
            //ResponseEntity.ok("""{"hello": "world", "request":"${it.body}"}""")
            ResponseEntity.ok(SampleOuterClass.Sample.newBuilder()
                .setHello("world")
                .setRequest(it.body)
                .build())
        }
    }
}