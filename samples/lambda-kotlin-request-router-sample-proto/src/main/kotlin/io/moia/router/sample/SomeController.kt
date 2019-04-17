package io.moia.router.sample

import io.moia.router.Request
import io.moia.router.ResponseEntity
import io.moia.router.proto.sample.SampleOuterClass

class SomeController {

    fun get(request: Request<Unit>) =
        ResponseEntity.ok(SampleOuterClass.Sample.newBuilder().setHello("hello").build())
}