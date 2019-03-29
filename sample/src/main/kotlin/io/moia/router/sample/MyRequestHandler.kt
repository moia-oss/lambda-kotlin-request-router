package io.moia.router.sample

import io.moia.router.Request
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router
import io.moia.router.proto.sample.SampleOuterClass

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