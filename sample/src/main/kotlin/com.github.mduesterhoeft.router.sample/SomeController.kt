package com.github.mduesterhoeft.router.sample

import com.github.mduesterhoeft.router.ApiRequest
import com.github.mduesterhoeft.router.Request
import com.github.mduesterhoeft.router.ResponseEntity

class SomeController {

    fun get(request: Request<Unit>) =
        ResponseEntity.ok(MyResponse("hello"))

    data class MyResponse(val hello: String)
}