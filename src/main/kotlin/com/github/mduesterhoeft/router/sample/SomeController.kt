package com.github.mduesterhoeft.router.sample

import com.github.mduesterhoeft.router.ApiRequest
import com.github.mduesterhoeft.router.ResponseEntity

class SomeController {

    fun get(request: ApiRequest) =
        ResponseEntity.ok(MyResponse("hello", request.body))

    data class MyResponse(val hello: String, val request: String?)
}