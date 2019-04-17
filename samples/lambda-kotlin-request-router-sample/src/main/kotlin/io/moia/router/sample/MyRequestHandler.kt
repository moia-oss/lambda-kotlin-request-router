package io.moia.router.sample

import io.moia.router.Request
import io.moia.router.RequestHandler
import io.moia.router.ResponseEntity
import io.moia.router.Router.Companion.router

class MyRequestHandler : RequestHandler() {
    private val controller = SomeController()

    override val router = router {
        // functions can be externalized...
        GET("/some", controller::get)

        // simple handlers can also be declared inline
        POST("/some") { r: Request<Sample> -> ResponseEntity.ok(r.body) }
    }
}