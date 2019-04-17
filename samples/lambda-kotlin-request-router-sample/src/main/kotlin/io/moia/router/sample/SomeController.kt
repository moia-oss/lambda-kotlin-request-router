package io.moia.router.sample

import io.moia.router.Request
import io.moia.router.ResponseEntity

class SomeController {

    fun get(request: Request<Unit>) =
        ResponseEntity.ok(Sample("hello"))
}

data class Sample(val hello: String)