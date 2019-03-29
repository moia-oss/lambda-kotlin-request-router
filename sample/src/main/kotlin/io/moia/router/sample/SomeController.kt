package io.moia.router.sample

import io.moia.router.Request
import io.moia.router.ResponseEntity

class SomeController {

    fun get(request: Request<Unit>) =
        ResponseEntity.ok(MyResponse("hello"))

    data class MyResponse(val hello: String)
}