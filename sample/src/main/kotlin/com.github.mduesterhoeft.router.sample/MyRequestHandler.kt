package com.github.mduesterhoeft.router.sample

import com.github.mduesterhoeft.router.Request
import com.github.mduesterhoeft.router.RequestHandler
import com.github.mduesterhoeft.router.ResponseEntity
import com.github.mduesterhoeft.router.Router.Companion.router
import com.github.mduesterhoeft.router.sample.proto.SampleOuterClass

class MyRequestHandler : RequestHandler() {
    private val controller = SomeController()

    override val router = router {
        GET("/some", controller::get)
        POST("/some") { r: Request<String> ->
            ResponseEntity.ok(SampleOuterClass.Sample.newBuilder()
                .setHello("world")
                .setRequest(r.body)
                .build())
        }
    }
}